package lt.pow.nukagit.db.dao;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.UUID;

public interface UsersDao {
    @SqlUpdate("INSERT INTO users (id, username) VALUES (UUID(), :username)" +
            " ON DUPLICATE KEY UPDATE username = username")
    void upsertUser(@Bind("username") String username);

    @SqlQuery("SELECT id FROM users WHERE username = :username AND not_archived = true")
    UUID getUserIdByName(@Bind("username") String username);

    @Transaction
    default UUID upsertUserAndGetId(String username) {
        upsertUser(username);
        return getUserIdByName(username);
    }
}
