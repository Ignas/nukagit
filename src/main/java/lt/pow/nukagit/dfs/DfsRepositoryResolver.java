package lt.pow.nukagit.dfs;

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DfsRepositoryResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(DfsRepositoryResolver.class);
  private final ConcurrentHashMap<String, Repository> repositoryCache;

  @Inject
  public DfsRepositoryResolver() {
    repositoryCache = new ConcurrentHashMap<>();
  }

  public synchronized Repository resolveDfsRepository(String username, String[] args) {
    LOGGER.debug("resolveDfsRepository: username={}, args={}", username, args);
    return repositoryCache.computeIfAbsent(
        args[1], (key) -> new InMemoryRepository(new DfsRepositoryDescription(key)));
  }

  public synchronized List<String> listRepositories() {
    return List.copyOf(repositoryCache.keySet());
  }
}
