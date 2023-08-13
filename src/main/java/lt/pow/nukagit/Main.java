package lt.pow.nukagit;

import com.google.common.io.Closer;
import io.opencensus.stats.Measure;
import lt.pow.nukagit.lib.lifecycle.Managed;
import lt.pow.nukagit.lib.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private static final Measure.MeasureLong startupCounter =
      Metrics.registerCounter("startup", "Startup counter");

  public static void main(String[] args) {
    var component = DaggerMainComponent.create();

    LOGGER.info("Starting up");

    try (Closer closer = Closer.create()) {
      // Using Closer even though it is designed around streams, because it is the only helper
      // library I found
      component.servers().stream().map(closer::register).forEach(Managed::start);
      Metrics.count(startupCounter);
      Thread.currentThread().join();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
