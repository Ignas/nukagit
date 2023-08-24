package lt.pow.nukagit.dfs;

import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;

import java.util.Objects;
import java.util.UUID;

public class NukagitDfsRepositoryDescription extends DfsRepositoryDescription {
  private final UUID repositoryId;

  public NukagitDfsRepositoryDescription(UUID id, String name) {
    super(name);
    this.repositoryId = id;
  }

  public UUID getRepositoryId() {
    return repositoryId;
  }

  @Override
  public boolean equals(Object b) {
    if (b instanceof NukagitDfsRepositoryDescription) {
      return super.equals(b)
          && this.repositoryId == ((NukagitDfsRepositoryDescription) b).repositoryId;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), repositoryId);
  }
}
