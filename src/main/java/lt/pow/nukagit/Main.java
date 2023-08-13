package lt.pow.nukagit;

import org.apache.sshd.common.util.security.bouncycastle.BouncyCastleGeneratorHostKeyProvider;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.IOException;
import java.nio.file.Path;


public class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    var sshServer = SshServer.setUpDefaultServer();
    sshServer.setHost("127.0.0.1");
    sshServer.setPort(2222);
    var keyPairGenerator = new SimpleGeneratorHostKeyProvider(Path.of("keys/ssh_host_key.pem"));
    keyPairGenerator.setAlgorithm("RSA");
    sshServer.setKeyPairProvider(keyPairGenerator);
    sshServer.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
    sshServer.setCommandFactory(new GitDfsPackCommandFactory());
    sshServer.start();

    // Sleep for maxint seconds
    Thread.sleep(Integer.MAX_VALUE);
  }
}
