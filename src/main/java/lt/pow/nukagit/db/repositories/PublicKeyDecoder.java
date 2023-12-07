package lt.pow.nukagit.db.repositories;

import lt.pow.nukagit.db.entities.ImmutablePublicKeyData;
import lt.pow.nukagit.db.entities.PublicKeyData;

import javax.inject.Inject;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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

    public PublicKeyData decodePublicKey(String keyLine) throws InvalidKeyStringException {
        String[] parts = keyLine.split(" ");
        for (String part : parts) {
            if (part.startsWith("AAAA")) {
                byte[] decodeBuffer = Base64.getDecoder().decode(part.getBytes(StandardCharsets.UTF_8));
                ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
                /* using 4 bytes from bb to generate integer which gives us length of key-
                format type, in this case len=7 as "ssh-rsa" has 7 chars
                */
                int len = bb.getInt();
                byte[] type = new byte[len];
                bb.get(type);
                String typeString = new String(type, StandardCharsets.UTF_8);
                if ("ssh-rsa".equals(typeString)) {
                    // extracting exponent and modulus from remaining byte-buffer
                    BigInteger exponent = decodeBigInt(bb);
                    BigInteger modulus = decodeBigInt(bb);
                    PublicKeyData keyData = ImmutablePublicKeyData.builder().modulus(modulus).exponent(exponent).build();
                    // Make sure a key can be constructed from the data
                    keyData.key();
                    return keyData;
                } else {
                    throw new InvalidPublicKeyTypeException(String.format("Only supports RSA keys, received %s key", typeString));
                }
            }
        }
        throw new InvalidKeyStringException("Key string missing the actual key");
    }
}
