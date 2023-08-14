package lt.pow.nukagit.dfs;

import java.io.IOException;
import org.eclipse.jgit.internal.storage.dfs.DfsObjDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsReaderOptions;
import org.eclipse.jgit.internal.storage.dfs.DfsReftableDatabase;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.internal.storage.reftable.ReftableConfig;

public class NukagitDfsRepository extends DfsRepository {
  private final DfsReaderOptions readerOptions;
  private final DfsObjDatabase objDb;
  private final org.eclipse.jgit.lib.RefDatabase refDb;

  public NukagitDfsRepository(DfsRepositoryBuilder builder) {
    super(builder);
    this.readerOptions = builder.getReaderOptions();
    // Should I create these whenever they are retrieved?
    // TODO: Test different block sizes
    objDb = new NukagitDfsObjDatabase(this, this.readerOptions, 1024);
    refDb = new RefDatabase(this);
  }

  public DfsObjDatabase getObjectDatabase() {
    return objDb;
  }

  public org.eclipse.jgit.lib.RefDatabase getRefDatabase() {
    return refDb;
  }

  public static class Builder extends DfsRepositoryBuilder<Builder, NukagitDfsRepository> {
    @Override
    public NukagitDfsRepository build() throws IOException {
      return new NukagitDfsRepository(this);
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
  }
}
