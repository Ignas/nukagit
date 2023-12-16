package lt.pow.nukagit.ssh;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import lt.pow.nukagit.db.repositories.PublicKeyRepository;
import lt.pow.nukagit.dfs.DfsRepositoryResolver;
import lt.pow.nukagit.dfs.GitDfsPackCommandFactory;
import lt.pow.nukagit.lib.lifecycle.Managed;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;

import javax.inject.Singleton;
import java.nio.file.Path;

@Module
public abstract class SshModule {

  @Provides
  static SshServerConfig configuration(Gestalt configurationProvider) {
    try {
      return configurationProvider.getConfig("ssh", SshServerConfig.class);
    } catch (GestaltException e) {
      throw new RuntimeException("SSH Server configuration is missing!", e);
    }
  }

  @Binds
  @IntoSet
  abstract Managed bindServer(ManagedSshServer server);

  @Provides
  @Singleton
  static SshServer createServer(
          DfsRepositoryResolver dfsRepositoryResolver,
          SshServerConfig sshServerConfig,
          PublicKeyRepository publicKeyRepository) {
    var sshServer = SshServer.setUpDefaultServer();
    sshServer.setHost(sshServerConfig.hostname());
    sshServer.setPort(sshServerConfig.port());
    var keyPairGenerator = new SimpleGeneratorHostKeyProvider(Path.of(sshServerConfig.hostKey()));
    keyPairGenerator.setAlgorithm(sshServerConfig.hostKeyAlgorithm());
    sshServer.setKeyPairProvider(keyPairGenerator);
    sshServer.setPublickeyAuthenticator(new UsernameResolvingPublickeyAuthenticator(
          publicKeyRepository.getPublicKeySupplier()
    ));
    sshServer.setCommandFactory(
        new GitDfsPackCommandFactory().withDfsRepositoryResolver(dfsRepositoryResolver));
    return sshServer;
  }
}
