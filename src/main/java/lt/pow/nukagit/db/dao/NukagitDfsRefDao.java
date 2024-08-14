package lt.pow.nukagit.db.dao;

import lt.pow.nukagit.db.entities.DfsRef;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.*;

import java.util.List;
import java.util.UUID;

public interface NukagitDfsRefDao {

    @SqlUpdate("INSERT IGNORE INTO refs (name, target, symbolic, object_id, peeled_ref, peeled, repository_id) " +
            "VALUES (:name, :target, :isSymbolic, :objectID, :peeledRef, :isPeeled, :repositoryId)")
    int create(@Bind("repositoryId") UUID repositoryId, @BindMethods DfsRef newDfsRefEntity);

    @SqlUpdate("UPDATE refs SET target = :new.target, symbolic = :new.isSymbolic, object_id = :new.objectID, peeled_ref = :new.peeledRef, peeled = :new.isPeeled " +
            "WHERE repository_id = :repositoryId AND name = :old.name AND symbolic = :old.isSymbolic " +
            "AND ((:old.target IS NULL AND target IS NULL) OR (:old.target IS NOT NULL AND target = :old.target)) " +
            "AND ((:old.objectID IS NULL AND object_id IS NULL) OR (:old.objectID IS NOT NULL AND object_id = :old.objectID))")
    int compareAndPut(@Bind("repositoryId") UUID repositoryId, @BindMethods("old") DfsRef oldDfsRef, @BindMethods("new") DfsRef newDfsRef);

    @SqlUpdate("DELETE FROM refs WHERE repository_id = :repositoryId AND name = :name AND symbolic = :isSymbolic " +
            "AND ((:target IS NULL AND target IS NULL) OR (:target IS NOT NULL AND target = :target)) " +
            "AND ((:objectID IS NULL AND object_id IS NULL) OR (:objectID IS NOT NULL AND object_id = :objectID))")
    int compareAndRemove(@Bind("repositoryId") UUID repositoryId, @BindMethods DfsRef oldDfsRefEntity);

    @SqlQuery("SELECT name, target, symbolic, object_id, peeled_ref, peeled " +
            "FROM refs WHERE repository_id = :repositoryId")
    List<DfsRef> listRefs(@Bind("repositoryId") UUID repositoryId);
}
