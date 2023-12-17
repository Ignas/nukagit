package lt.pow.nukagit.ssh;

import org.apache.sshd.server.command.AbstractCommandSupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class WhoAmICommand extends AbstractCommandSupport {
    public WhoAmICommand(String command) {
        super(command, null);
    }

    @Override
    public void run() {
        String username = (String) this.getServerSession().getIoSession().getAttribute("username");
        try {
            this.getOutputStream().write(String.format("You are: %s%n", username).getBytes(StandardCharsets.UTF_8));
            onExit(0);
        } catch (IOException e) {
            onExit(1);
        }
    }
}
