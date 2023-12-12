package lt.pow.nukagit.db.repositories;

import lt.pow.nukagit.db.entities.ImmutablePublicKeyData;
import lt.pow.nukagit.db.entities.PublicKeyData;

import javax.inject.Inject;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.*;
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

    private ECPoint decodeECPoint(ByteBuffer bb) {
        int len = bb.getInt();
        var format = bb.get();

        // validating that the key is uncompressed
        if (format != 0x04) {
            throw new IllegalArgumentException("Only uncompressed points are supported");
        }

        byte[] qBytes = new byte[len - 1];
        bb.get(qBytes);

        byte[] xBytes = new byte[qBytes.length / 2];
        byte[] yBytes = new byte[qBytes.length / 2];
        System.arraycopy(qBytes, 0, xBytes, 0, xBytes.length);
        System.arraycopy(qBytes, xBytes.length, yBytes, 0, yBytes.length);
        return new ECPoint(new BigInteger(xBytes), new BigInteger(yBytes));
    }

    public PublicKeyData decodePublicKey(String keyLine) throws InvalidKeyStringException {
        String[] parts = keyLine.split(" ");
        PublicKey key;
        PublicKeyData.KeyType keyType;

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
                    keyType = PublicKeyData.KeyType.RSA;
                    // extracting exponent and modulus from remaining byte-buffer
                    BigInteger exponent = decodeBigInt(bb);
                    BigInteger modulus = decodeBigInt(bb);

                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    try {
                        // Generate public key
                        key = KeyFactory.getInstance("RSA").generatePublic(spec);
                    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                        throw new InvalidKeyStringException("Failed to construct a valid RSA key", e);
                    }
                } else if (typeString.startsWith("ecdsa-sha2-")) {
                    keyType = PublicKeyData.KeyType.EC;
                    // extracting curve name from remaining byte-buffer
                    String nistNameString = decodeString(bb);
                    String nameString = nistNameString.replace("nist", "sec") + "r1";

                    // Generate ECPoint
                    ECPoint ecPoint = decodeECPoint(bb);

                    try {
                        // Generate ECParameterSpec
                        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
                        parameters.init(new ECGenParameterSpec(nameString));
                        ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
                        // Generate ECPublicKeySpec
                        ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParameterSpec);
                        // Generate public key
                        key = KeyFactory.getInstance("EC").generatePublic(spec);
                    } catch (InvalidParameterSpecException | InvalidKeySpecException | NoSuchAlgorithmException e) {
                        throw new InvalidKeyStringException("Failed to construct a valid ECDSA key", e);
                    }

                } else {
                    throw new InvalidPublicKeyTypeException(String.format("Only supports RSA and ECDSA keys, received %s key", typeString));
                }
                return ImmutablePublicKeyData.builder()
                        .keyType(keyType)
                        .keyBytes(key.getEncoded())
                        .build();
            }
        }
        throw new InvalidKeyStringException("Key string missing the actual key");
    }
}
