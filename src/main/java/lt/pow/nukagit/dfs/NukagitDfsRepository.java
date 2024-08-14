package lt.pow.nukagit.dfs;

import lt.pow.nukagit.db.dao.NukagitDfsObjDao;
import lt.pow.nukagit.db.dao.NukagitDfsRefDao;
import lt.pow.nukagit.db.entities.DfsRef;
import lt.pow.nukagit.db.entities.ImmutableDfsRef;
import lt.pow.nukagit.minio.NukagitBlockRepository;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.internal.storage.dfs.*;
import org.eclipse.jgit.internal.storage.reftable.ReftableConfig;
import org.eclipse.jgit.internal.storage.reftable.ReftableDatabase;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.util.RefList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class NukagitDfsRepository extends DfsRepository {
    public final static ThreadLocal<String> USERNAME
            = new ThreadLocal<>();
    private final DfsReaderOptions readerOptions;
    private final DfsObjDatabase objDb;
    private final org.eclipse.jgit.lib.RefDatabase refDb;
    private final NukagitDfsObjDao dfsObjDao;

    private final NukagitDfsRefDao dfsRefDao;
    private final NukagitBlockRepository blockRepository;

    public NukagitDfsRepository(Builder builder) {
        super(builder);
        this.readerOptions = builder.getReaderOptions();
        this.dfsObjDao = builder.getDfsDao();
        this.dfsRefDao = builder.getDfsRefDao();
        this.blockRepository = builder.getBlockRepository();
        objDb = new NukagitDfsObjDatabase(this, dfsObjDao, blockRepository, this.readerOptions, blockRepository.getBlockSize());
        refDb = new RelationalRefDatabase(this, dfsRefDao);
    }

    public DfsObjDatabase getObjectDatabase() {
        return objDb;
    }

    public org.eclipse.jgit.lib.RefDatabase getRefDatabase() {
        return refDb;
    }

    public static class Builder extends DfsRepositoryBuilder<Builder, NukagitDfsRepository> {
        private final NukagitDfsObjDao dfsDao;
        private final NukagitDfsRefDao dfsRefDao;
        private final NukagitBlockRepository blockRepository;

        public Builder(NukagitDfsObjDao dfsObjDao, NukagitDfsRefDao dfsRefDao, NukagitBlockRepository blockRepository) {
            super();
            this.dfsDao = dfsObjDao;
            this.dfsRefDao = dfsRefDao;
            this.blockRepository = blockRepository;
        }

        @Override
        public NukagitDfsRepository build() throws IOException {
            return new NukagitDfsRepository(this);
        }

        public NukagitDfsObjDao getDfsDao() {
            return dfsDao;
        }

        public NukagitDfsRefDao getDfsRefDao() {
            return dfsRefDao;
        }

        public NukagitBlockRepository getBlockRepository() {
            return blockRepository;
        }
    }

    public static class RefDatabase extends DfsReftableDatabase {
        protected RefDatabase(DfsRepository repo) {
            super(repo);
        }

        @Override
        public ReftableConfig getReftableConfig() {
            ReftableConfig cfg = new ReftableConfig();
            cfg.setAlignBlocks(true);
            cfg.setIndexObjects(true);
            cfg.fromConfig(getRepository().getConfig());
            return cfg;
        }

        @Override
        protected boolean compareAndPut(Ref oldRef, @Nullable Ref newRef)
                throws IOException {
            // Add settings for a repository
            // Pass along username somehow
            ReceiveCommand cmd = ReftableDatabase.toCommand(oldRef, newRef);
            try (RevWalk rw = new RevWalk(getRepository())) {
                rw.setRetainBody(false);
                newBatchUpdate().setAllowNonFastForwards(true).addCommand(cmd)
                        .execute(rw, NullProgressMonitor.INSTANCE);
            }
            return switch (cmd.getResult()) {
                case OK -> true;
                case REJECTED_OTHER_REASON -> throw new IOException(cmd.getMessage());
                default -> false;
            };
        }
    }

    public static class RelationalRefDatabase extends DfsRefDatabase {
        private final NukagitDfsRefDao dfsRefDao;

        protected RelationalRefDatabase(DfsRepository repo, NukagitDfsRefDao dfsRefDao) {
            super(repo);
            this.dfsRefDao = dfsRefDao;
        }

        UUID getRepositoryId() {
            return ((NukagitDfsRepositoryDescription) getRepository().getDescription())
                    .getRepositoryId();
        }

        private Ref toJgitRef(HashMap<String, DfsRef> allRefMap, DfsRef dfsRef) {
            if (dfsRef.isSymbolic()) {
                DfsRef target = allRefMap.get(dfsRef.target());
                return new SymbolicRef(dfsRef.name(), toJgitRef(allRefMap, target));
            }
            return new ObjectIdRef.Unpeeled(
                    Ref.Storage.LOOSE, dfsRef.name(), ObjectId.fromString(dfsRef.objectID()));
        }

        private DfsRef toDfsRef(Ref ref) {
            if (ref.isSymbolic()) {
                return ImmutableDfsRef.builder()
                        .name(ref.getName())
                        .isSymbolic(true)
                        .target(ref.getTarget().getName())
                        .build();
            }
            return ImmutableDfsRef.builder()
                    .name(ref.getName())
                    .isSymbolic(false)
                    .objectID(ref.getObjectId().name())
                    .target(null)
                    .peeledRef(ref.getPeeledObjectId() != null ? ref.getPeeledObjectId().name() : null)
                    .isPeeled(ref.isPeeled())
                    .build();
        }

        @Override
        protected RefCache scanAllRefs() throws IOException {
            var allRefs = dfsRefDao.listRefs(getRepositoryId());
            var allRefMap = new HashMap<String, DfsRef>();
            for (var ref : allRefs) {
                allRefMap.put(ref.name(), ref);
            }

            ArrayList<Ref> symRefList = new ArrayList<>();
            ArrayList<Ref> idRefList = new ArrayList<>();
            for (var ref : allRefs) {
                if (ref.isSymbolic()) {
                    symRefList.add(toJgitRef(allRefMap, ref));
                } else {
                    idRefList.add(toJgitRef(allRefMap, ref));
                }
            }

            var idRefListBuilder = new RefList.Builder<>();
            idRefListBuilder.addAll(idRefList.toArray(new Ref[0]), 0, idRefList.size());
            idRefListBuilder.sort();
            RefList<Ref> ids = idRefListBuilder.toRefList();


            var symRefListBuilder = new RefList.Builder<>();
            symRefListBuilder.addAll(symRefList.toArray(new Ref[0]), 0, symRefList.size());
            symRefListBuilder.sort();
            RefList<Ref> sym = symRefListBuilder.toRefList();
            return new RefCache( ids, sym);
        }

        @Override
        protected boolean compareAndPut(Ref oldRef, Ref newRef) throws IOException {
            if (oldRef.getStorage() == Ref.Storage.NEW) {
                return dfsRefDao.create(getRepositoryId(), toDfsRef(newRef)) == 1;
            }
            return dfsRefDao.compareAndPut(getRepositoryId(), toDfsRef(oldRef), toDfsRef(newRef)) == 1;
        }

        @Override
        protected boolean compareAndRemove(Ref oldRef) throws IOException {
            return dfsRefDao.compareAndRemove(getRepositoryId(), toDfsRef(oldRef)) == 1;
        }
    }
}
