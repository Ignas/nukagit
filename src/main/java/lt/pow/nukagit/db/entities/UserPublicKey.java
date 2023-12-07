package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

@Value.Immutable
public interface UserPublicKey {
  String username();

  BigInteger exponent();

  BigInteger modulus();

  String fingerprint();

  default PublicKey key() {
    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus(), exponent());
    PublicKey key;
    try {
      key = KeyFactory.getInstance("RSA").generatePublic(spec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
      return key;
  }
}
