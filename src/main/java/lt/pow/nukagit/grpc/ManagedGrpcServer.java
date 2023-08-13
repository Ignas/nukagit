package lt.pow.nukagit.grpc;

import io.grpc.Server;
import lt.pow.nukagit.lib.lifecycle.Managed;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class ManagedGrpcServer implements Managed {
  private final Server grpcServer;

  @Inject
  public ManagedGrpcServer(Server server) {
    grpcServer = server;
  }

  public void start() {
    try {
      grpcServer.start();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start GRPC Server!", e);
    }
  }

  @Override
  public void close() throws IOException {
    if (grpcServer != null) {
      grpcServer.shutdown();
      try {
        grpcServer.awaitTermination();
      } catch (InterruptedException e) {
        throw new RuntimeException("Failed to cleanly shut down GRPC Server!", e);
      }
    }
  }
}
