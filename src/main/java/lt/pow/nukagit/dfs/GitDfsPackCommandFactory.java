package lt.pow.nukagit.dfs;

import org.apache.sshd.git.pack.GitPackCommand;
import org.apache.sshd.git.pack.GitPackCommandFactory;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.slf4j.MDC;

public class GitDfsPackCommandFactory extends GitPackCommandFactory {
    private DfsRepositoryResolver dfsRepositoryResolver;

    @Override
    protected Command executeSupportedCommand(ChannelSession channel, String command) {
        MDC.put("username", (String) channel.getServerSession().getIoSession().getAttribute("username"));
        MDC.put("command", command);
        return super.executeSupportedCommand(channel, command);
    }

    @Override
    public GitPackCommand createGitCommand(String command) {
        return new GitDfsPackCommand(command, resolveExecutorService(command), dfsRepositoryResolver);
    }

    public GitDfsPackCommandFactory withDfsRepositoryResolver(DfsRepositoryResolver dfsRepositoryResolver) {
        this.dfsRepositoryResolver = dfsRepositoryResolver;
        return this;
    }
}
