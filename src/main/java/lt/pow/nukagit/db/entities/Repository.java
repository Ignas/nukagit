package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

@Value.Immutable
public interface Repository {
    String name();
}
