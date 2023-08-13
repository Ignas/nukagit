package lt.pow.nukagit;

import dagger.Component;
import javax.inject.Singleton;

import io.grpc.Server;
import lt.pow.nukagit.dfs.DfsModule;
import lt.pow.nukagit.grpc.GrpcModule;
import lt.pow.nukagit.grpc.GrpcWorker;
import lt.pow.nukagit.prometheus.PrometheusModule;
import lt.pow.nukagit.prometheus.PrometheusWorker;
import lt.pow.nukagit.ssh.SshModule;
import org.apache.sshd.server.SshServer;

@Component(modules = {SshModule.class, DfsModule.class, PrometheusModule.class, GrpcModule.class})
@Singleton
public interface MainComponent {
  SshServer sshServer();

  PrometheusWorker prometheusServer();

  GrpcWorker grpcServer();
}
