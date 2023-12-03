package lt.pow.nukagit.dfs;

import io.minio.MinioClient;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
  private final MinioClient minio;

  @Inject
  public DfsRepositoryResolver(NukagitDfsDao dfsDao, MinioClient minio) {
    this.dfsDao = dfsDao;
    this.minio = minio;
    repositoryCache = new ConcurrentHashMap<>();
  }

  @WithSpan
  public synchronized Repository resolveDfsRepository(String username, String[] args)
      throws IOException {
    LOGGER.debug("resolveDfsRepository: username={}, args={}", username, args);
    String repositoryName = args[1];

    if (!repositoryName.startsWith("/memory/")) {
      var id = dfsDao.getRepositoryIdByName(repositoryName);
      if (id == null) {
        throw new IOException(String.format("Repository with the name %s does not exist!", repositoryName));
      }
      return new NukagitDfsRepository.Builder(dfsDao, minio)
          .setRepositoryDescription(new NukagitDfsRepositoryDescription(id, repositoryName))
          // .withPath(new Path("testRepositories", name))
          // .withBlockSize(64)
          // .withReplication((short) 2)
          .setReaderOptions(new DfsReaderOptions())
          .build();
    }
    // Keep in memory repositories for simple testing
    return repositoryCache.computeIfAbsent(
        repositoryName, (key) -> new InMemoryRepository(new DfsRepositoryDescription(key)));
  }

  public synchronized List<String> listRepositories() {
    return List.copyOf(repositoryCache.keySet());
  }
}
