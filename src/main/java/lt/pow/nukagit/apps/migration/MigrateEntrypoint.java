package lt.pow.nukagit.apps.migration;

import dagger.Lazy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import lt.pow.nukagit.lib.cli.CliCommand;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@Singleton
@CommandLine.Command(name = "migrate", description = "Run database migrations")
public class MigrateEntrypoint implements CliCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateEntrypoint.class);
  private final Lazy<DataSource> dataSource;

  @Inject
  public MigrateEntrypoint(Lazy<DataSource> dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  @WithSpan
  public void run() {
    LOGGER.info("Running Migrations!");
    Flyway flyway = Flyway.configure().dataSource(dataSource.get()).load();
    flyway.repair();
    flyway.migrate();
  }
}
