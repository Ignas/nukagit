package lt.pow.nukagit.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
abstract class DatabaseTestBase extends Specification {
    @Shared
    MySQLContainer mysql = new MySQLContainer("mysql:8")
            .withDatabaseName("nukagit")
            .withUsername("nukagit")
            .withPassword("nukagit")

    HikariDataSource dataSource
    Jdbi jdbi

    void setup() {
        HikariConfig hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(mysql.jdbcUrl)
        hikariConfig.setUsername(mysql.username)
        hikariConfig.setPassword(mysql.password)

        // Create HikariCP DataSource
        dataSource = new HikariDataSource(hikariConfig)

        // Create JDBI instance
        jdbi = DatabaseModule.jdbi(dataSource)

        // Run migrations
        Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load()
        flyway.clean()
        flyway.migrate()
    }

    void cleanup() {
        dataSource.close()
    }
}
