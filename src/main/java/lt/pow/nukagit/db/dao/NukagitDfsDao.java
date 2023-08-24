package lt.pow.nukagit.db.dao;

import lt.pow.nukagit.db.entities.Pack;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;
import java.util.UUID;

public interface NukagitDfsDao {
  @SqlUpdate(
      "INSERT INTO repositories (id, name) VALUES (UUID(), :name) "
          + "ON DUPLICATE KEY UPDATE name = name")
  void upsertRepository(@Bind("name") String name);

  @SqlQuery("SELECT id FROM repositories WHERE name = :name")
  UUID getRepositoryIdByName(@Bind("name") String name);

  @SqlQuery("SELECT push_id FROM repositories WHERE id = :repositoryId")
  UUID getLastPush(@Bind("repositoryId") UUID repositoryId);

  @Transaction
  default UUID upsertRepositoryAndGetId(String name) {
    upsertRepository(name);
    return getRepositoryIdByName(name);
  }

  @SqlQuery(
      "SELECT * FROM packs WHERE push_id = (select push_id from repositories where id = :repositoryId)")
  List<Pack> listPacks(@Bind("repositoryId") UUID repositoryId);

  @SqlUpdate("INSERT INTO pushes (id, repository_id) VALUES (:pushId, :repositoryId)")
  void createPush(@Bind("repositoryId") UUID repositoryId, @Bind("pushId") UUID pushId);

  @SqlBatch(
      "INSERT INTO packs (push_id, name, source, ext, file_size, object_count) VALUES (:pushId, :name, :source, :ext, :file_size, :object_count)")
  @BatchChunkSize(50)
  void insertPacks(@Bind("pushId") UUID pushId, @BindMethods List<Pack> packs);

  @SqlUpdate(
      "INSERT INTO packs (push_id, name, source, ext, file_size, object_count) "
          + "SELECT :toPushId, name, source, ext, file_size, object_count "
          + "FROM packs WHERE push_id = :fromPushId")
  void copyPacks(@Bind("fromPushId") UUID fromPushId, @Bind("toPushId") UUID toPushId);

  @SqlBatch(
      "DELETE FROM packs WHERE push_id = :pushId AND name = :name AND source = :source AND ext = :ext")
  @BatchChunkSize(50)
  void deletePacks(@Bind("pushId") UUID pushId, @BindMethods List<Pack> replace);

  @SqlUpdate("UPDATE repositories SET push_id = :pushId WHERE id = :repositoryId")
  void setPush(@Bind("repositoryId") UUID repositoryId, @Bind("pushId") UUID pushId);

  @Transaction
  default void commitPack(UUID repositoryId, List<Pack> desc, List<Pack> replace) {
    // Get last push
    UUID lastPush = getLastPush(repositoryId);
    // Create a new push
    UUID pushId = UUID.randomUUID();
    createPush(repositoryId, pushId);
    // Insert new packs
    insertPacks(pushId, desc);
    // Copy over previous commit
    copyPacks(lastPush, pushId);
    // Remove replaced packs
    deletePacks(pushId, replace);
    // Set the new push as the last push
    setPush(repositoryId, pushId);
  }
}
