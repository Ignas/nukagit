package lt.pow.nukagit;

import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.git.AbstractGitCommand;
import org.apache.sshd.git.pack.GitPackCommand;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UploadPack;

import java.nio.file.Path;
import java.util.List;

public class GitDfsPackCommand extends GitPackCommand {

  public GitDfsPackCommand(String command, CloseableExecutorService executorService) {
    super((cmd, args, session, fs) -> Path.of("/"), command, executorService);
  }

  @Override
  public void run() {
    String command = getCommand();
    try {
      List<String> strs = parseDelimitedString(command, " ", true);
      String[] args = strs.toArray(new String[strs.size()]);
      for (int i = 0; i < args.length; i++) {
        String argVal = args[i];
        if (argVal.startsWith("'") && argVal.endsWith("'")) {
          args[i] = argVal.substring(1, argVal.length() - 1);
          argVal = args[i];
        }
        if (argVal.startsWith("\"") && argVal.endsWith("\"")) {
          args[i] = argVal.substring(1, argVal.length() - 1);
        }
      }

      if (args.length != 2) {
        throw new IllegalArgumentException("Invalid git command line (no arguments): " + command);
      }

      var db = resolveDfsRepository(args);
      String subCommand = args[0];
      if (RemoteConfig.DEFAULT_UPLOAD_PACK.equals(subCommand)) {
        new UploadPack(db).upload(getInputStream(), getOutputStream(), getErrorStream());
      } else if (RemoteConfig.DEFAULT_RECEIVE_PACK.equals(subCommand)) {
        new ReceivePack(db).receive(getInputStream(), getOutputStream(), getErrorStream());
      } else {
        throw new IllegalArgumentException("Unknown git command: " + command);
      }

      onExit(0);
    } catch (Throwable t) {
      onExit(-1, t.getClass().getSimpleName());
    }
  }

  private Repository resolveDfsRepository(String[] args) {
    // getServerSession() to get the username
    return new InMemoryRepository(new DfsRepositoryDescription(args[1]));
  }
}
