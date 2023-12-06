package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

@Value.Immutable
public interface PublicKey {
  String username();

  String publicKey();

  String fingerprint();
}
