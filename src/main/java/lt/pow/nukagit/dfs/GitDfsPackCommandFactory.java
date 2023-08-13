package lt.pow.nukagit.dfs;

import org.apache.sshd.git.pack.GitPackCommand;
import org.apache.sshd.git.pack.GitPackCommandFactory;

public class GitDfsPackCommandFactory extends GitPackCommandFactory {
  private DfsRepositoryResolver dfsRepositoryResolver;

  @Override
  public GitPackCommand createGitCommand(String command) {
    return new GitDfsPackCommand(command, resolveExecutorService(command), dfsRepositoryResolver);
  }

  public GitDfsPackCommandFactory withDfsRepositoryResolver(DfsRepositoryResolver dfsRepositoryResolver) {
    this.dfsRepositoryResolver = dfsRepositoryResolver;
    return this;
  }
}
