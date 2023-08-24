package lt.pow.nukagit.dfs;

import lt.pow.nukagit.db.dao.NukagitDfsDao;
import org.eclipse.jgit.internal.storage.dfs.DfsReaderOptions;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DfsRepositoryResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(DfsRepositoryResolver.class);
  private final ConcurrentHashMap<String, Repository> repositoryCache;
  private final NukagitDfsDao dfsDao;

  @Inject
  public DfsRepositoryResolver(NukagitDfsDao dfsDao) {
    this.dfsDao = dfsDao;
    repositoryCache = new ConcurrentHashMap<>();
  }

  public synchronized Repository resolveDfsRepository(String username, String[] args)
      throws IOException {
    LOGGER.debug("resolveDfsRepository: username={}, args={}", username, args);
    String repositoryName = args[1];

    if (repositoryName.startsWith("/minio/")) {
      var id = dfsDao.upsertRepositoryAndGetId(repositoryName);
      return new NukagitDfsRepository.Builder(dfsDao)
          .setRepositoryDescription(new NukagitDfsRepositoryDescription(id, repositoryName))
          // .withPath(new Path("testRepositories", name))
          // .withBlockSize(64)
          // .withReplication((short) 2)
          .setReaderOptions(new DfsReaderOptions())
          .build();
    }
    return repositoryCache.computeIfAbsent(
        repositoryName, (key) -> new InMemoryRepository(new DfsRepositoryDescription(key)));
  }

  public synchronized List<String> listRepositories() {
    return List.copyOf(repositoryCache.keySet());
  }
}
