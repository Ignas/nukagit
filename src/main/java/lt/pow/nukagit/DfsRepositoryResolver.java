package lt.pow.nukagit;

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class DfsRepositoryResolver {

  static private Logger LOGGER = LoggerFactory.getLogger(DfsRepositoryResolver.class);
  private final ConcurrentHashMap<String, Repository> repositoryCache;

  public DfsRepositoryResolver() {
    repositoryCache = new ConcurrentHashMap<>();
  }

  synchronized Repository resolveDfsRepository(String username, String[] args) {
    LOGGER.debug("resolveDfsRepository: username={}, args={}", username, args);
    return repositoryCache.computeIfAbsent(args[1], (key) -> new InMemoryRepository(new DfsRepositoryDescription(key)));
  }
}
