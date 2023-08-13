package lt.pow.nukagit.prometheus;

import io.opencensus.contrib.grpc.metrics.RpcViews;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class PrometheusWorker implements AutoCloseable {

  private final HTTPServer.Builder builder;
  private HTTPServer server;

  @Inject
  public PrometheusWorker() {
    int port = 9090;
    builder = new HTTPServer.Builder().withPort(port);
  }

  public void start() {
    DefaultExports.initialize();
    RpcViews.registerServerGrpcViews();
    PrometheusStatsCollector.createAndRegister();

    try {
      server = builder.build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    server.close();
  }
}
