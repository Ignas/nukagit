package lt.pow.nukagit.apps.server;

import com.google.common.io.Closer;
import dagger.Lazy;
import io.opencensus.stats.Measure;
import lt.pow.nukagit.lib.cli.CliCommand;
import lt.pow.nukagit.lib.lifecycle.Managed;
import lt.pow.nukagit.lib.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Singleton
@CommandLine.Command(name = "serve", description = "Start the server")
public class ServerEntrypoint implements CliCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerEntrypoint.class);

  private static final Measure.MeasureLong startupCounter =
      Metrics.registerCounter("startup", "Startup counter");
  private final Lazy<Set<Managed>> servers;

  @Inject
  public ServerEntrypoint(Lazy<Set<Managed>> servers) {
    this.servers = servers;
  }

  @Override
  public void run() {
    LOGGER.info("Starting up");

    try (Closer closer = Closer.create()) {
      // Using Closer even though it is designed around streams, because it is the only helper
      // library I found
      servers.get().stream().map(closer::register).forEach(Managed::start);
      Metrics.count(startupCounter);
      Thread.currentThread().join();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
