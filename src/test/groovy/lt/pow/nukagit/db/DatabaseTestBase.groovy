package lt.pow.nukagit.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import lt.pow.nukagit.db.entities.Pack
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource

@Testcontainers
abstract class DatabaseTestBase extends Specification {
    @Shared
    MySQLContainer mysql = new MySQLContainer("mysql:8")
            .withDatabaseName("nukagit")
            .withUsername("nukagit")
            .withPassword("nukagit")

    Jdbi jdbi

    void setup() {
        HikariConfig hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(mysql.jdbcUrl)
        hikariConfig.setUsername(mysql.username)
        hikariConfig.setPassword(mysql.password)

        // Create HikariCP DataSource
        DataSource dataSource = new HikariDataSource(hikariConfig)

        // Create JDBI instance
        jdbi = DatabaseModule.jdbi(dataSource)

        // Run migrations
        Flyway flyway = Flyway.configure().dataSource(dataSource).load()
        flyway.migrate()
    }
}
