package lt.pow.nukagit.dfs;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.git.pack.GitPackCommand;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UploadPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitDfsPackCommand extends GitPackCommand {
    Logger LOGGER = LoggerFactory.getLogger(GitDfsPackCommand.class);
    private final DfsRepositoryResolver dfsRepositoryResolver;

    public GitDfsPackCommand(
            String command,
            CloseableExecutorService executorService,
            DfsRepositoryResolver dfsRepositoryResolver) {
        super((cmd, args, session, fs) -> Path.of("/"), command, executorService);
        this.dfsRepositoryResolver = dfsRepositoryResolver;
    }

    @Override
    @WithSpan
    public void run() {
        String username = (String) getServerSession().getIoSession().getAttribute("username");
        MDC.put("username", username);
        NukagitDfsRepository.USERNAME.set(username);

        String command = getCommand();
        try {
            var args = extractQuotedStrings(command);

            if (args.length != 2) {
                throw new IllegalArgumentException("Invalid git command line (no arguments): " + command);
            }

            var db = (DfsRepository) dfsRepositoryResolver.resolveDfsRepository(username, args);
            String subCommand = args[0];
            MDC.put("git.repository", db.getDescription().getRepositoryName());
            MDC.put("git.command", subCommand);

            if (RemoteConfig.DEFAULT_UPLOAD_PACK.equals(subCommand)) {
                uploadPack(db);
            } else if (RemoteConfig.DEFAULT_RECEIVE_PACK.equals(subCommand)) {
                receivePack(db);
                // if HEAD ref has not been set up yet, set it up
                if (db.getRefDatabase().exactRef(Constants.HEAD) == null) {
                    List<String> preferredRefs = List.of("main", "master");
                    boolean headSet = false;
                    for (String ref : preferredRefs) {
                        if (db.getRefDatabase().exactRef(Constants.R_HEADS + ref) != null) {
                            RefUpdate u = db.updateRef(Constants.HEAD);
                            u.link(Constants.R_HEADS + ref);
                            headSet = true;
                            break;
                        }
                    }
                    // If no preferred refs exist, link HEAD to the only ref if there is exactly one
                    if (!headSet && db.getRefDatabase().getRefs().size() == 1) {
                        RefUpdate u = db.updateRef(Constants.HEAD);
                        u.link(db.getRefDatabase().getRefs().get(0).getName());
                    }                }
            } else {
                throw new IllegalArgumentException("Unknown git command: " + command);
            }

            onExit(0);
        } catch (Throwable t) {
            LOGGER.error("Error running git command: {}", command, t);
            onExit(-1, t.getClass().getSimpleName());
        } finally {
            MDC.remove("username");
            MDC.remove("command");
            MDC.remove("git.repository");
            MDC.remove("git.command");
            NukagitDfsRepository.USERNAME.remove();
        }
    }

    @WithSpan
    private void receivePack(Repository db) throws IOException {
        new ReceivePack(db).receive(getInputStream(), getOutputStream(), getErrorStream());
    }

    @WithSpan
    private void uploadPack(Repository db) throws IOException {
        try(var uploadOperation = new UploadPack(db)) {
            uploadOperation.upload(getInputStream(), getOutputStream(), getErrorStream());
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
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    insideQuotes = true;
                } else if (!Character.isWhitespace(c)) {
                    currentString.append(c);
                } else if (!currentString.isEmpty()) {
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

        if (!currentString.isEmpty()) {
            splitStrings.add(currentString.toString());
        }

        return splitStrings.toArray(new String[0]);
    }
}
