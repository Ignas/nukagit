package lt.pow.nukagit;

import com.google.common.annotations.VisibleForTesting;
import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.git.pack.GitPackCommand;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UploadPack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitDfsPackCommand extends GitPackCommand {

  private final DfsRepositoryResolver dfsRepositoryResolver;

  public GitDfsPackCommand(String command, CloseableExecutorService executorService, DfsRepositoryResolver dfsRepositoryResolver) {
    super((cmd, args, session, fs) -> Path.of("/"), command, executorService);
    this.dfsRepositoryResolver = dfsRepositoryResolver;
  }

  @Override
  public void run() {
    String command = getCommand();
    try {
      var args = extractQuotedStrings(command);

      if (args.length != 2) {
        throw new IllegalArgumentException("Invalid git command line (no arguments): " + command);
      }

      var db = dfsRepositoryResolver.resolveDfsRepository(getServerSession().getUsername(), args);
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

  @VisibleForTesting
  public static String[] extractQuotedStrings(String input) {
    List<String> splitStrings = new ArrayList<>();

    StringBuilder currentString = new StringBuilder();
    boolean insideQuotes = false;
    boolean escaped = false;

    for (char c : input.toCharArray()) {
      if (!insideQuotes) {
        if (escaped) {
          currentString.append(c);
          escaped = false;
        }  else if (c == '\\') {
          escaped = true;
        } else if (c == '\'') {
          insideQuotes = true;
        } else if (!Character.isWhitespace(c)) {
          currentString.append(c);
        } else if (currentString.length() > 0) {
          splitStrings.add(currentString.toString());
          currentString.setLength(0);
        }
      } else {
        if (escaped) {
          currentString.append(c);
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == '\'') {
          insideQuotes = false;
        } else {
          currentString.append(c);
        }
      }
    }

    if (currentString.length() > 0) {
      splitStrings.add(currentString.toString());
    }

    return splitStrings.toArray(new String[0]);
  }
}
