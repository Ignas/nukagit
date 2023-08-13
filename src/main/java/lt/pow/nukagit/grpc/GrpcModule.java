package lt.pow.nukagit.grpc;

import dagger.Module;
import dagger.Provides;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;

@Module
public class GrpcModule {

  @Provides
  Server createGrpcServer(RepositoriesService repositoriesService) {
    HealthStatusManager healthStatusManager = new HealthStatusManager();
    return ServerBuilder.forPort(50051)
        .addService(repositoriesService)
        .addService(ProtoReflectionService.newInstance())
        .addService(healthStatusManager.getHealthService())
        .build();
  }
}
