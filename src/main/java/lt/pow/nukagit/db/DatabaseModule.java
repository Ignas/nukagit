package lt.pow.nukagit.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import javax.sql.DataSource;

import lt.pow.nukagit.db.dao.*;
import lt.pow.nukagit.db.entities.*;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.mapper.reflect.SnakeCaseColumnNameMatcher;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

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
  static Jdbi jdbi(DataSource dataSource) {
    var jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
    jdbi.registerArgument(new UUIDArgumentFactory());
    jdbi.registerArgument(new BigIntegerArgumentFactory());
    jdbi.registerColumnMapper(new BigIntegerColumnMapper());
    JdbiImmutables jdbiImmutables = jdbi.getConfig(JdbiImmutables.class);
    jdbiImmutables.registerImmutable(Pack.class);
    jdbiImmutables.registerImmutable(DfsRef.class);
    jdbiImmutables.registerImmutable(Repository.class);
    jdbiImmutables.registerImmutable(UserPublicKey.class);
    jdbiImmutables.registerModifiable(UserPublicKey.class);
    jdbiImmutables.registerImmutable(PublicKeyData.class);
    return jdbi;
  }

  @Provides
  @Singleton
  NukagitDfsRepositoryDao repositoryDao(Jdbi jdbi) {
    return jdbi.onDemand(NukagitDfsRepositoryDao.class);
  }

  @Provides
  @Singleton
  NukagitDfsObjDao dfsObjDao(Jdbi jdbi) {
    return jdbi.onDemand(NukagitDfsObjDao.class);
  }

  @Provides
  @Singleton
  NukagitDfsRefDao dfsRefDao(Jdbi jdbi) {
    return jdbi.onDemand(NukagitDfsRefDao.class);
  }

  @Provides
  @Singleton
  UsersDao usersDao(Jdbi jdbi) {
    return jdbi.onDemand(UsersDao.class);
  }

  @Provides
  @Singleton
  PublicKeysDao publicKeysDao(Jdbi jdbi) {
    return jdbi.onDemand(PublicKeysDao.class);
  }

}
