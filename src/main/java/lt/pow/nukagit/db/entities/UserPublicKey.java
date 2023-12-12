package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;
import org.jdbi.v3.core.mapper.Nested;

@Value.Immutable
@Value.Modifiable
public interface UserPublicKey {
  String username();

  @Nested
  PublicKeyData publicKeyData();
}
