package lt.pow.nukagit.ssh;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lt.pow.nukagit.lib.lifecycle.Managed;
import org.apache.sshd.server.SshServer;

@Singleton
public class ManagedSshServer implements Managed {
  private final SshServer sshServer;

  @Inject
  public ManagedSshServer(SshServer server) {
    sshServer = server;
  }

  public void start() {
    try {
      sshServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start SSH Server!", e);
    }
  }

  @Override
  public void close() throws IOException {
    if (sshServer != null) {
      sshServer.stop();
    }
  }
}
