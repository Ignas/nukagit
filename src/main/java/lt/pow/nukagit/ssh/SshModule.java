package lt.pow.nukagit.ssh;

import dagger.Module;
import dagger.Provides;
import lt.pow.nukagit.dfs.DfsRepositoryResolver;
import lt.pow.nukagit.dfs.GitDfsPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.nio.file.Path;

@Module
public class SshModule {
  @Provides
  SshServer createServer(DfsRepositoryResolver dfsRepositoryResolver) {
    var sshServer = SshServer.setUpDefaultServer();
    sshServer.setHost("127.0.0.1");
    sshServer.setPort(2222);
    var keyPairGenerator = new SimpleGeneratorHostKeyProvider(Path.of("keys/ssh_host_key.pem"));
    keyPairGenerator.setAlgorithm("RSA");
    sshServer.setKeyPairProvider(keyPairGenerator);
    sshServer.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
    sshServer.setCommandFactory(
        new GitDfsPackCommandFactory().withDfsRepositoryResolver(dfsRepositoryResolver));
    return sshServer;
  }
}
