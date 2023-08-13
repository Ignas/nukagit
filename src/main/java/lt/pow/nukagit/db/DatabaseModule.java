package lt.pow.nukagit.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import lt.pow.nukagit.db.command.RepositoriesCommandDao;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.inject.Singleton;

@Module
public class DatabaseModule {
  @Provides
  DatabaseConfiguration databaseConfiguration(Gestalt gestalt) {
    try {
      return gestalt.getConfig("database", DatabaseConfiguration.class);
    } catch (GestaltException e) {
      throw new RuntimeException("Failed to load database config", e);
    }
  }

  @Provides
  @Singleton
  HikariDataSource dataSource(DatabaseConfiguration configuration) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(configuration.jdbcUrl());
    hikariConfig.setUsername(configuration.username());
    hikariConfig.setPassword(configuration.password());
    return new HikariDataSource(hikariConfig);
  }

  @Provides
  @Singleton
  Jdbi jdbi(HikariDataSource dataSource) {
    return Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
  }

  @Provides
  @Singleton
  RepositoriesCommandDao repositoriesCommandDao(Jdbi jdbi) {
    return jdbi.onDemand(RepositoriesCommandDao.class);
  }
}
