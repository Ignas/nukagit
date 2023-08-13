package lt.pow.nukagit.config;

import dagger.Module;
import dagger.Provides;

import java.nio.file.Path;
import java.util.List;

import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.ClassPathConfigSource;
import org.github.gestalt.config.source.EnvironmentConfigSource;
import org.github.gestalt.config.source.FileConfigSource;
import org.github.gestalt.config.tag.Tags;

@Module
public class ConfigModule {

  @Provides
  Gestalt configurationProvider() {
    Gestalt gestalt;
    try {
      gestalt =
          new GestaltBuilder()
              .addSource(
                  new ClassPathConfigSource(
                      "/default.properties")) // Load the default property files from resources.
              .addSource(new FileConfigSource(Path.of("config/application.yaml")))
              .addSource(new RobustEnvironmentConfigSource("NUKAGIT_", true))
              .build();
      gestalt.loadConfigs();
    } catch (GestaltException e) {
      throw new RuntimeException("Failed to load configuration!", e);
    }
    return gestalt;
  }
}
