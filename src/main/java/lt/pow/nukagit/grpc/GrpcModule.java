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
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;

import javax.inject.Singleton;

@Module
public abstract class GrpcModule {

    @Binds
    @IntoSet
    abstract Managed bindServer(ManagedGrpcServer server);

    @Provides
    static GrpcServerConfig configuration(Gestalt configurationProvider) {
        try {
            return configurationProvider.getConfig("grpc", GrpcServerConfig.class);
        } catch (GestaltException e) {
            throw new RuntimeException("GRPC Server configuration is missing!", e);
        }
    }

    @Provides
    @Singleton
    static Server createGrpcServer(RepositoriesService repositoriesService, UsersService usersService, GrpcServerConfig config) {
        HealthStatusManager healthStatusManager = new HealthStatusManager();
        return ServerBuilder.forPort(config.port())
                .addService(repositoriesService)
                .addService(usersService)
                .addService(ProtoReflectionService.newInstance())
                .addService(healthStatusManager.getHealthService())
                .build();
    }
}
