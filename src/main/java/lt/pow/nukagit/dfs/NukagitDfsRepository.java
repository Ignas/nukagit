package lt.pow.nukagit.dfs;

import lt.pow.nukagit.db.dao.NukagitDfsDao;
import lt.pow.nukagit.minio.NukagitBlockRepository;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.internal.storage.dfs.*;
import org.eclipse.jgit.internal.storage.reftable.ReftableConfig;
import org.eclipse.jgit.internal.storage.reftable.ReftableDatabase;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;

import java.io.IOException;

public class NukagitDfsRepository extends DfsRepository {
    public final static ThreadLocal<String> USERNAME
            = new ThreadLocal<>();
    private final DfsReaderOptions readerOptions;
    private final DfsObjDatabase objDb;
    private final org.eclipse.jgit.lib.RefDatabase refDb;
    private final NukagitDfsDao dfsDao;
    private final NukagitBlockRepository blockRepository;

    public NukagitDfsRepository(Builder builder) {
        super(builder);
        this.readerOptions = builder.getReaderOptions();
        this.dfsDao = builder.getDfsDao();
        this.blockRepository = builder.getBlockRepository();
        objDb = new NukagitDfsObjDatabase(this, dfsDao, blockRepository, this.readerOptions, blockRepository.getBlockSize());
        refDb = new RefDatabase(this);
    }

    public DfsObjDatabase getObjectDatabase() {
        return objDb;
    }

    public org.eclipse.jgit.lib.RefDatabase getRefDatabase() {
        return refDb;
    }

    public static class Builder extends DfsRepositoryBuilder<Builder, NukagitDfsRepository> {
        private final NukagitDfsDao dfsDao;
        private final NukagitBlockRepository blockRepository;

        public Builder(NukagitDfsDao dfsDao, NukagitBlockRepository blockRepository) {
            super();
            this.dfsDao = dfsDao;
            this.blockRepository = blockRepository;
        }

        @Override
        public NukagitDfsRepository build() throws IOException {
            return new NukagitDfsRepository(this);
        }

        public NukagitDfsDao getDfsDao() {
            return dfsDao;
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
}
