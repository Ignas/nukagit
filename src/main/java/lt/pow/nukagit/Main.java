package lt.pow.nukagit;

import io.opencensus.stats.Measure;
import lt.pow.nukagit.lib.metrics.Metrics;

public class Main {
  private static final Measure.MeasureLong startupCounter =
      Metrics.registerCounter("startup", "Startup counter");

  public static void main(String[] args) {
    var component = DaggerMainComponent.create();

    try (var sshServer = component.sshServer();
        var prometheus = component.prometheusServer();
        var grpcServer = component.grpcServer()) {
      prometheus.start();
      sshServer.start();
      grpcServer.start();
      Metrics.count(startupCounter);
      Thread.currentThread().join();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
