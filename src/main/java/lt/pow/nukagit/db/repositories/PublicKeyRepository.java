package lt.pow.nukagit.db.repositories;

import com.google.common.base.Suppliers;
import lt.pow.nukagit.db.dao.PublicKeysDao;
import lt.pow.nukagit.db.dao.UsersDao;
import lt.pow.nukagit.db.entities.UserPublicKey;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PublicKeyRepository {
    private final UsersDao usersDao;
    private final PublicKeysDao publicKeysDao;
    private final UsernameValidator usernameValidator;

    private final PublicKeyDecoder publicKeyDecoder;

    private final Supplier<List<UserPublicKey>> publicKeyCache;

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

    public void addUserWithKey(String username, String publicKey) throws InvalidKeyStringException, InvalidUsernameException {
        // Validate that username is valid
        if (!usernameValidator.isValid(username)) {
            throw new InvalidUsernameException(String.format("Username %s is not considered valid", username));
        }

        // Parse public key
        var publicKeyData = publicKeyDecoder.decodePublicKey(publicKey);

        // Create the user and add the public key
        var userId = usersDao.upsertUserAndGetId(username);
        publicKeysDao.addPublicKey(userId, publicKeyData.fingerprint(), publicKeyData);
    }

    public Supplier<List<UserPublicKey>> getPublicKeySupplier() {
        return publicKeyCache;
    }
}
