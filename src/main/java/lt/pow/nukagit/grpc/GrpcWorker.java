package lt.pow.nukagit.grpc;

import io.grpc.Server;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class GrpcWorker implements AutoCloseable {
  private final Server grpcServer;

  @Inject
  public GrpcWorker(Server server) {
    grpcServer = server;
  }

  public void start() throws IOException {
    grpcServer.start();
  }

  @Override
  public void close() throws Exception {
    if (grpcServer != null) {
      grpcServer.shutdown();
      grpcServer.awaitTermination();
    }
  }
}
