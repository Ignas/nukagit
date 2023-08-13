package lt.pow.nukagit.apps.migration;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import lt.pow.nukagit.apps.server.ServerEntrypoint;
import lt.pow.nukagit.lib.cli.CliCommand;

@Module
public abstract class MigrateModule {
  @Binds
  @IntoSet
  public abstract CliCommand migrateCommand(MigrateEntrypoint migrateEntrypoint);
}
