package lt.pow.nukagit.db.dao;

import lt.pow.nukagit.db.entities.Pack;
import lt.pow.nukagit.db.entities.Repository;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.*;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;
import java.util.UUID;

public interface NukagitDfsRepositoryDao {
  @SqlUpdate(
      "INSERT INTO repositories (id, name) VALUES (UUID(), :name) "
          + "ON DUPLICATE KEY UPDATE name = name")
  void upsertRepository(@Bind("name") String name);

  @SqlQuery("SELECT * FROM repositories WHERE not_archived = true ORDER BY name")
  List<Repository> listRepositories();

  @SqlQuery("SELECT id FROM repositories WHERE name = :name AND not_archived = true")
  UUID getRepositoryIdByName(@Bind("name") String name);

  @Transaction
  default UUID upsertRepositoryAndGetId(String name) {
    upsertRepository(name);
    return getRepositoryIdByName(name);
  }

  @SqlUpdate("UPDATE repositories SET deleted_on = NOW() WHERE id = :repositoryId")
  void archiveRepository(@Bind("repositoryId") UUID repositoryId);
}
