package lt.pow.nukagit.minio;

import dagger.Module;
import dagger.Provides;
import io.minio.MinioClient;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;

import javax.inject.Singleton;

@Module
public class MinioModule {
  @Provides
  MinioConfiguration minioConfiguration(Gestalt gestalt) {
    try {
      return gestalt.getConfig("minio", MinioConfiguration.class);
    } catch (GestaltException e) {
      throw new RuntimeException("Failed to load Minio configuration", e);
    }
  }

  @Provides
  @Singleton
  MinioClient minioClient(MinioConfiguration configuration) {
    return MinioClient.builder()
        .endpoint(configuration.getEndpoint())
        .credentials(configuration.getAccessKey(), configuration.getSecretKey())
        .build();
  }
}
