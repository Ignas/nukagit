package lt.pow.nukagit;

import dagger.Component;
import io.grpc.Server;
import io.minio.MinioClient;
import lt.pow.nukagit.apps.migration.MigrateEntrypoint;
import lt.pow.nukagit.apps.migration.MigrateModule;
import lt.pow.nukagit.apps.server.ServerModule;
import lt.pow.nukagit.config.ConfigModule;
import lt.pow.nukagit.db.DatabaseModule;
import lt.pow.nukagit.dfs.DfsModule;
import lt.pow.nukagit.grpc.GrpcModule;
import lt.pow.nukagit.minio.MinioModule;
import lt.pow.nukagit.ssh.SshModule;
import org.apache.sshd.server.SshServer;

import javax.inject.Singleton;

@Component(
        modules = {
                SshModule.class,
                DfsModule.class,
                GrpcModule.class,
                ConfigModule.class,
                DatabaseModule.class,
                ServerModule.class,
                MigrateModule.class,
                MinioModule.class
        })
@Singleton
public interface TestComponent {
    SshServer sshServer();

    Server grpcServer();

    MigrateEntrypoint migrateEntrypoint();

    MinioClient minio();
}
