package lt.pow.nukagit.config;

import dagger.Module;
import dagger.Provides;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.ClassPathConfigSource;
import org.github.gestalt.config.source.EnvironmentConfigSource;
import org.github.gestalt.config.source.FileConfigSource;

import java.nio.file.Path;

@Module
public class ConfigModule {

  @Provides
  Gestalt configurationProvider() {
    String configPath = System.getProperty("nukagit.config_path", "config/application.yaml");
    Gestalt gestalt;
    try {
      gestalt =
          new GestaltBuilder()
              .addSource(
                  new ClassPathConfigSource(
                      "/default.properties")) // Load the default property files from resources.
              .addSource(new FileConfigSource(Path.of(configPath)))
              .addSource(new EnvironmentConfigSource("NUKAGIT_", true))
              .build();
      gestalt.loadConfigs();
    } catch (GestaltException e) {
      throw new RuntimeException("Failed to load configuration!", e);
    }
    return gestalt;
  }
}
