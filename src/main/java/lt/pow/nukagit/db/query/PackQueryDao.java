package lt.pow.nukagit.db.query;

import lt.pow.nukagit.db.entities.Pack;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface PackQueryDao {

  @SqlQuery("SELECT * FROM packs WHERE repository_id = :repositoryId")
  List<Pack> listPacks(@Bind("repositoryId") int repositoryId);
}
