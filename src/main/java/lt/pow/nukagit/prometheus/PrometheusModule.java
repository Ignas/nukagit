package lt.pow.nukagit.prometheus;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import lt.pow.nukagit.lib.lifecycle.Managed;

@Module
public abstract class PrometheusModule {
  @Binds
  @IntoSet
  abstract Managed bindServer(ManagedPrometheusServer server);
}
