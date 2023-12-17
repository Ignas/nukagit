package lt.pow.nukagit.db.entities;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.immutables.value.Value;
import org.jdbi.v3.core.annotation.JdbiProperty;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@Value.Immutable
public interface PublicKeyData {
    enum KeyType {
        RSA,
        EC
    }

    KeyType keyType();

    byte[] keyBytes();

    default PublicKey key() {
        var spec = new X509EncodedKeySpec(keyBytes());
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance(keyType().toString());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            return keyFactory.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @JdbiProperty(map = false)
    default String fingerprint() {
        return KeyUtils.getFingerPrint(key());
    }
}
