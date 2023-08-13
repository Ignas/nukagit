package lt.pow.nukagit.apps.migration;

import com.google.common.io.Closer;
import dagger.Lazy;
import io.opencensus.stats.Measure;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lt.pow.nukagit.lib.cli.CliCommand;
import lt.pow.nukagit.lib.lifecycle.Managed;
import lt.pow.nukagit.lib.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@Singleton
@CommandLine.Command(name = "migrate", description = "Run database migrations")
public class MigrateEntrypoint implements CliCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateEntrypoint.class);

  @Inject
  public MigrateEntrypoint() {}

  @Override
  public void run() {
    LOGGER.info("Running Migrations! (NOT)");
  }
}
