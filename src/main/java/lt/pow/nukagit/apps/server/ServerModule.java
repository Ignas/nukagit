package lt.pow.nukagit.apps.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import lt.pow.nukagit.lib.cli.CliCommand;

@Module
public abstract class ServerModule {
  @Binds
  @IntoSet
  public abstract CliCommand serverCommand(ServerEntrypoint serverEntrypoint);
}
