package lt.pow.nukagit.grpc;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import lt.pow.nukagit.lib.lifecycle.Managed;

@Module
public abstract class GrpcModule {

  @Binds
  @IntoSet
  abstract Managed bindServer(ManagedGrpcServer server);

  @Provides
  static Server createGrpcServer(RepositoriesService repositoriesService) {
    HealthStatusManager healthStatusManager = new HealthStatusManager();
    return ServerBuilder.forPort(50051)
        .addService(repositoriesService)
        .addService(ProtoReflectionService.newInstance())
        .addService(healthStatusManager.getHealthService())
        .build();
  }
}
