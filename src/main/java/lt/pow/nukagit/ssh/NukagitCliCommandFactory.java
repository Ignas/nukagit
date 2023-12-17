package lt.pow.nukagit.ssh;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.shell.UnknownCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;

public class NukagitCliCommandFactory implements CommandFactory {
    Logger LOGGER = LoggerFactory.getLogger(NukagitCliCommandFactory.class);

    @Override
    public Command createCommand(ChannelSession channel, String command) throws IOException {
        String username = (String) channel.getSession().getIoSession().getAttribute("username");
        MDC.put("username", username);
        MDC.put("command", command);
        LOGGER.info("User '{}' ran command '{}'", username, command);
        if ("whoami".equals(command.strip()))
            return new WhoAmICommand(command);
        else
            return UnknownCommandFactory.INSTANCE.createCommand(channel, command);
    }
}
