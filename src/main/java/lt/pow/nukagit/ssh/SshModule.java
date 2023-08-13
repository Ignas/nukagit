package lt.pow.nukagit.ssh;

import dagger.Module;
import dagger.Provides;
import lt.pow.nukagit.dfs.DfsRepositoryResolver;
import lt.pow.nukagit.dfs.GitDfsPackCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;

import java.nio.file.Path;

@Module
public class SshModule {

  @Provides
  SshServerConfig configuration(Gestalt configurationProvider) {
    try {
      return configurationProvider.getConfig("ssh", SshServerConfig.class);
    } catch (GestaltException e) {
      throw new RuntimeException("SSH Server configuration is missing!", e);
    }
  }

  @Provides
  SshServer createServer(
      DfsRepositoryResolver dfsRepositoryResolver, SshServerConfig sshServerConfig) {
    var sshServer = SshServer.setUpDefaultServer();
    sshServer.setHost(sshServerConfig.hostname());
    sshServer.setPort(sshServerConfig.port());
    var keyPairGenerator = new SimpleGeneratorHostKeyProvider(Path.of(sshServerConfig.hostKey()));
    keyPairGenerator.setAlgorithm(sshServerConfig.hostKeyAlgorithm());
    sshServer.setKeyPairProvider(keyPairGenerator);
    sshServer.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
    sshServer.setCommandFactory(
        new GitDfsPackCommandFactory().withDfsRepositoryResolver(dfsRepositoryResolver));
    return sshServer;
  }
}
