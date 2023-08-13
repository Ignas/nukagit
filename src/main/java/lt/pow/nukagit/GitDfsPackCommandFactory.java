package lt.pow.nukagit;

import org.apache.sshd.git.pack.GitPackCommand;
import org.apache.sshd.git.pack.GitPackCommandFactory;

public class GitDfsPackCommandFactory extends GitPackCommandFactory {
  @Override
  public GitPackCommand createGitCommand(String command) {
    return new GitDfsPackCommand(command, resolveExecutorService(command));
  }
}
