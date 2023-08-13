package lt.pow.nukagit;

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;

import java.util.concurrent.ConcurrentHashMap;

public class DfsRepositoryResolver {
  private final ConcurrentHashMap<String, Repository> repositoryCache;

  public DfsRepositoryResolver() {
    repositoryCache = new ConcurrentHashMap<>();
  }

  synchronized Repository resolveDfsRepository(String username, String[] args) {
    return repositoryCache.computeIfAbsent(args[1], (key) -> new InMemoryRepository(new DfsRepositoryDescription(key)));
  }
}
