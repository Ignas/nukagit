package lt.pow.nukagit;

import dagger.Component;
import javax.inject.Singleton;
import lt.pow.nukagit.apps.server.ServerEntrypoint;
import lt.pow.nukagit.config.ConfigModule;
import lt.pow.nukagit.db.DatabaseModule;
import lt.pow.nukagit.dfs.DfsModule;
import lt.pow.nukagit.grpc.GrpcModule;
import lt.pow.nukagit.prometheus.PrometheusModule;
import lt.pow.nukagit.ssh.SshModule;

@Component(
    modules = {
      SshModule.class,
      DfsModule.class,
      PrometheusModule.class,
      GrpcModule.class,
      ConfigModule.class,
      DatabaseModule.class
    })
@Singleton
public interface MainComponent {
  ServerEntrypoint server();
}
