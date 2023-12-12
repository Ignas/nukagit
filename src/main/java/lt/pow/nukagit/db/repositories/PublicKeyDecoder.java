package lt.pow.nukagit.db.repositories;

import lt.pow.nukagit.db.entities.ImmutablePublicKeyData;
import lt.pow.nukagit.db.entities.PublicKeyData;

import javax.inject.Inject;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.ECPoint;
import java.util.Base64;

public class PublicKeyDecoder {
    @Inject
    public PublicKeyDecoder() {
    }

    private BigInteger decodeBigInt(ByteBuffer bb) {
        // use first 4 bytes to generate an Integer that gives the length of bytes to create BigInteger
        int len = bb.getInt();
        byte[] bytes = new byte[len];
        bb.get(bytes);
        return new BigInteger(bytes);
    }

    private String decodeString(ByteBuffer bb) {
        // use first 4 bytes to generate an Integer that gives the length of bytes to create String
        int len = bb.getInt();
        byte[] bytes = new byte[len];
        bb.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private ECPoint getECPoint(BigInteger q) {
        byte[] qBytes = q.toByteArray();
        if (qBytes[0] != 0x04) {
            throw new IllegalArgumentException("Only uncompressed points are supported");
        }
        byte[] xBytes = new byte[qBytes.length / 2];
        byte[] yBytes = new byte[qBytes.length / 2];
        System.arraycopy(qBytes, 0, xBytes, 0, xBytes.length);
        System.arraycopy(qBytes, xBytes.length, yBytes, 0, yBytes.length);
        return new ECPoint(new BigInteger(xBytes), new BigInteger(yBytes));
    }

    public PublicKeyData decodePublicKey(String keyLine) throws InvalidKeyStringException {
        String[] parts = keyLine.split(" ");
        for (String part : parts) {
            if (part.startsWith("AAAA")) {
                byte[] decodeBuffer;
                try {
                    decodeBuffer = Base64.getDecoder().decode(part.getBytes(StandardCharsets.UTF_8));
                } catch (IllegalArgumentException e) {
                    throw new InvalidKeyStringException("Key string is not a valid base64 string", e);
                }

                ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
                String typeString = decodeString(bb);
                if ("ssh-rsa".equals(typeString)) {
                    // extracting exponent and modulus from remaining byte-buffer
                    BigInteger exponent = decodeBigInt(bb);
                    BigInteger modulus = decodeBigInt(bb);
                    PublicKeyData keyData = ImmutablePublicKeyData.builder()
                            .keyType(PublicKeyData.KeyType.RSA)
                            .modulus(modulus)
                            .exponent(exponent).build();
                    // Make sure a key can be constructed from the data
                    keyData.key();
                    return keyData;
                } else if (typeString.startsWith("ecdsa-sha2-")) {
                    // extracting curve name from remaining byte-buffer
                    String nistNameString = decodeString(bb);
                    String nameString = nistNameString.replace("nist", "sec") + "r1";
                    // extracting q from remaining byte-buffer
                    BigInteger q = decodeBigInt(bb);
                    PublicKeyData keyData = ImmutablePublicKeyData.builder()
                            .keyType(PublicKeyData.KeyType.ECDSA)
                            .name(nameString)
                            .x(getECPoint(q).getAffineX())
                            .y(getECPoint(q).getAffineY())
                            .build();
                    // Make sure a key can be constructed from the data
                    keyData.key();
                    return keyData;
                } else {
                    throw new InvalidPublicKeyTypeException(String.format("Only supports RSA and ECDSA keys, received %s key", typeString));
                }
            }
        }
        throw new InvalidKeyStringException("Key string missing the actual key");
    }
}
