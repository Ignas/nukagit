package lt.pow.nukagit.db.command;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface RepositoriesCommandDao {
  @SqlUpdate(
      "INSERT INTO repositories (name, description) VALUES (:name, :description) "
          + "ON CONFLICT (name) DO UPDATE SET description = :description")
  void upsertRepository(String name, String description);
}
