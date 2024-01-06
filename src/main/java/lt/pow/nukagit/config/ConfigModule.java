package lt.pow.nukagit.config;

import dagger.Module;
import dagger.Provides;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.*;

import java.nio.file.Path;

@Module
public class ConfigModule {

    @Provides
    Gestalt configurationProvider() {
        Path configPath = Path.of(System.getProperty("nukagit.config_path", "config/application.yaml"));
        Gestalt gestalt;
        try {
            var gestaltBuilder =
                    new GestaltBuilder()
                            .addSource(
                                    ClassPathConfigSourceBuilder.builder()
                                            .setResource("/default.properties").build()); // Load the default property files from resources.

            // check if the config file exists
            if (configPath.toFile().exists()) {
                gestaltBuilder.addSource(FileConfigSourceBuilder.builder().setPath(configPath).build());
            }
            gestaltBuilder.addSource(EnvironmentConfigSourceBuilder.builder().setPrefix("NUKAGIT_").setRemovePrefix(true).build());
            gestalt = gestaltBuilder.build();
            gestalt.loadConfigs();
        } catch (GestaltException e) {
            throw new RuntimeException("Failed to load configuration!", e);
        }
        return gestalt;
    }
}
