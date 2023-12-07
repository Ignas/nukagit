package lt.pow.nukagit.db.repositories;

import com.google.common.base.Suppliers;
import lt.pow.nukagit.db.dao.PublicKeysDao;
import lt.pow.nukagit.db.dao.UsersDao;
import lt.pow.nukagit.db.entities.ImmutablePublicKeyData;
import lt.pow.nukagit.db.entities.PublicKeyData;
import lt.pow.nukagit.db.entities.UserPublicKey;

import javax.inject.Inject;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PublicKeyRepository {
    static SecureRandom random = new SecureRandom();

    private final UsersDao usersDao;
    private final PublicKeysDao publicKeysDao;
    private final UsernameValidator usernameValidator;

    private final PublicKeyDecoder publicKeyDecoder;

    private final Supplier<List<UserPublicKey>> publicKeyCache;

    static public PublicKeyData generateRandomPublicKeyData() {
        var modulusBytes = new byte[128];
        random.nextBytes(modulusBytes);

        var exponentBytes = new byte[8];
        random.nextBytes(exponentBytes);

        return ImmutablePublicKeyData.builder()
                .modulus(new BigInteger(modulusBytes).abs())
                .exponent(new BigInteger(exponentBytes).abs())
                .build();
    }

    @Inject
    public PublicKeyRepository(UsersDao usersDao, PublicKeysDao publicKeysDao, UsernameValidator usernameValidator, PublicKeyDecoder publicKeyDecoder) {
        this.usersDao = usersDao;
        this.publicKeysDao = publicKeysDao;
        this.publicKeyCache = Suppliers.memoizeWithExpiration(
                publicKeysDao::listPublicKeys,
                10,
                TimeUnit.MINUTES
        );
        this.usernameValidator = usernameValidator;
        this.publicKeyDecoder = publicKeyDecoder;
    }

    void addUserWithKey(String username, String publicKey) throws InvalidKeyStringException, InvalidUsernameException {
        // Validate that username is valid
        if (!usernameValidator.isValid(username)) {
            throw new InvalidUsernameException(String.format("Username %s is not considered valid", username));
        }

        // Parse public key
        var publicKeyData = publicKeyDecoder.decodePublicKey(publicKey);

        // Create the user and add the public key
        var userId = usersDao.upsertUserAndGetId(username);
        publicKeysDao.addPublicKey(userId, publicKeyData.fingerprint(), publicKeyData.exponent(), publicKeyData.modulus());
    }

    Collection<UserPublicKey> getPublicKeyCollection() {
        return publicKeyCache.get();
    }
}
