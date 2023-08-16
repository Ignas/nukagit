package lt.pow.nukagit.db.command;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface RepositoriesCommandDao {
  @SqlUpdate(
      "INSERT INTO repositories (name, description) VALUES (:name, :description) "
          + "ON DUPLICATE KEY UPDATE description = :description")
  void upsertRepository(@Bind("name") String name, @Bind("description") String description);

  @SqlQuery("SELECT id FROM repositories WHERE name = :name")
  long getRepositoryIdByName(@Bind("name") String name);

  @Transaction
  default Long upsertAndGetId(String name, String description) {
    upsertRepository(name, description);
    return getRepositoryIdByName(name);
  }
}
