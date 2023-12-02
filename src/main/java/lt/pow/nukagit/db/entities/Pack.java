package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

@Value.Immutable
public interface Pack {
  String name();

  String source();

  String ext();

  long file_size();

  long object_count();

  long min_update_index();

  long max_update_index();
}
