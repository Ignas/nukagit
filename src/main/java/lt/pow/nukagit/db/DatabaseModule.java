package lt.pow.nukagit.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import lt.pow.nukagit.db.command.RepositoriesCommandDao;
import lt.pow.nukagit.db.query.PackQueryDao;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.inject.Singleton;
import javax.sql.DataSource;

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
  DataSource dataSource(DatabaseConfiguration configuration) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(configuration.jdbcUrl());
    hikariConfig.setUsername(configuration.username());
    hikariConfig.setPassword(configuration.password());
    return new HikariDataSource(hikariConfig);
  }

  @Provides
  @Singleton
  Jdbi jdbi(DataSource dataSource) {
    return Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
  }

  @Provides
  @Singleton
  RepositoriesCommandDao repositoriesCommandDao(Jdbi jdbi) {
    return jdbi.onDemand(RepositoriesCommandDao.class);
  }

  @Provides
  @Singleton
  PackQueryDao packQueryDao(Jdbi jdbi) {
    return jdbi.onDemand(PackQueryDao.class);
  }
}
