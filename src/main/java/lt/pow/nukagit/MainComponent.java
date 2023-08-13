package lt.pow.nukagit;

import dagger.Component;
import java.util.List;
import java.util.Set;
import javax.inject.Singleton;
import lt.pow.nukagit.config.ConfigModule;
import lt.pow.nukagit.dfs.DfsModule;
import lt.pow.nukagit.grpc.GrpcModule;
import lt.pow.nukagit.lib.lifecycle.Managed;
import lt.pow.nukagit.prometheus.PrometheusModule;
import lt.pow.nukagit.ssh.SshModule;

@Component(
    modules = {
      SshModule.class,
      DfsModule.class,
      PrometheusModule.class,
      GrpcModule.class,
      ConfigModule.class
    })
@Singleton
public interface MainComponent {
  Set<Managed> servers();
}
