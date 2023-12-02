package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

@Value.Immutable
public interface Pack {
  String name();

  String source();

  String ext();

  long fileSize();

  long objectCount();

  long minUpdateIndex();

  long maxUpdateIndex();
}
