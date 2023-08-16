package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

@Value.Immutable
public interface Pack {

  int repository_id();

  String name();

  String source();

  String ext();
}
