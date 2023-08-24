package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
public interface Pack {
  String name();

  String source();

  String ext();

  long file_size();

  long object_count();
}
