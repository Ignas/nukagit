package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

@Value.Immutable
public interface PublicKeyData {
    BigInteger modulus();

    BigInteger exponent();

    static PublicKeyData generateRandom() {
        SecureRandom random = new SecureRandom();
        var modulusBytes = new byte[128];
        random.nextBytes(modulusBytes);

        var exponentBytes = new byte[8];
        random.nextBytes(exponentBytes);

        return ImmutablePublicKeyData.builder()
                .modulus(new BigInteger(modulusBytes).abs())
                .exponent(new BigInteger(exponentBytes).abs())
                .build();
    }

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
