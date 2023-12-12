package lt.pow.nukagit.db.entities;

import org.immutables.value.Value;
import org.jdbi.v3.core.annotation.JdbiProperty;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.Objects;

@Value.Immutable
public interface PublicKeyData {
    enum KeyType {
        RSA,
        ECDSA
    }

    KeyType keyType();

    @Nullable
    BigInteger modulus();

    @Nullable
    BigInteger exponent();

    @Nullable
    String name();

    @Nullable
    BigInteger x();

    @Nullable
    BigInteger y();

    default PublicKey key() {
        if (keyType().equals(KeyType.RSA)) {
            // Generate RSAPublicKeySpec
            RSAPublicKeySpec spec = new RSAPublicKeySpec(Objects.requireNonNull(modulus()), Objects.requireNonNull(exponent()));
            PublicKey key;
            try {
                // Generate public key
                key = KeyFactory.getInstance("RSA").generatePublic(spec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            return key;
        } else if (keyType().equals(KeyType.ECDSA)) {
            try {
                // Generate ECPoint
                ECPoint ecPoint = new ECPoint(Objects.requireNonNull(x()), Objects.requireNonNull(y()));
                // Generate ECParameterSpec
                AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
                parameters.init(new ECGenParameterSpec(Objects.requireNonNull(name())));
                ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
                // Generate ECPublicKeySpec
                ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
                // Generate public key
                return KeyFactory.getInstance("EC").generatePublic(spec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unknown key type");
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
