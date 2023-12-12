package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;
import org.jdbi.v3.core.annotation.JdbiProperty;

import java.security.KeyFactory;
import java.security.MessageDigest;
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

    /**
     * Generate fingerprint from public key
     * <p>
     * This function generates a SHA1 fingerprint from the public key.
     * But the algorithm is different from the openssh fingerprint algorithm.
     *
     * @return fingerprint
     */
    @JdbiProperty(map = false)
    default String fingerprint() {
        byte[] encoded = key().getEncoded();

        // SHA1
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(encoded);
        byte[] fingerprint = md.digest();

        // format fingerprint
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fingerprint.length; i++) {
            sb.append(String.format("%02X", fingerprint[i]));
            if (i < fingerprint.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }
}
