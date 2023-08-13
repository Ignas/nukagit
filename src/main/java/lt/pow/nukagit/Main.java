package lt.pow.nukagit;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    var component = DaggerMainComponent.create();

    try (var sshServer = component.sshServer();
        var prometheus = component.prometheusServer();
        var grpcServer = component.grpcServer()) {
      prometheus.start();
      sshServer.start();
      grpcServer.start();
      Thread.currentThread().join();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
