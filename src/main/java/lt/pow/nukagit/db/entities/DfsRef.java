package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public interface DfsRef {
  String name();

  @Nullable
  String target();

  boolean isSymbolic();

  @Nullable
  String objectID();

  @Nullable
  String peeledRef();

  boolean isPeeled();
}
