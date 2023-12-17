package lt.pow.nukagit.ssh;

import lt.pow.nukagit.db.entities.UserPublicKey;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.security.PublicKey;
import java.util.List;
import java.util.function.Supplier;

public class UsernameResolvingPublickeyAuthenticator implements PublickeyAuthenticator {

    Logger LOGGER = LoggerFactory.getLogger(UsernameResolvingPublickeyAuthenticator.class);
    private final Supplier<List<UserPublicKey>> keySetSupplier;

    public UsernameResolvingPublickeyAuthenticator(Supplier<List<UserPublicKey>> keySetSupplier) {
        this.keySetSupplier = keySetSupplier;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) throws AsyncAuthException {
        if (!username.equals("git")) {
            LOGGER.warn("User '{}' tried to authenticate with public key, but all users should" +
                    " use git as their username", username);
            return false;
        }
        var allKeys = keySetSupplier.get();
        LOGGER.info("Trying to authenticate user '{}' with public key against {} key(s)", username, allKeys.size());
        for (var userPublicKey : allKeys) {
            if (KeyUtils.compareKeys(userPublicKey.publicKeyData().key(), key)) {
                session.getIoSession().setAttribute("username", userPublicKey.username());
                MDC.put("username", userPublicKey.username());
                LOGGER.info("User '{}' authenticated with public key '{}'", userPublicKey.username(), userPublicKey.publicKeyData().fingerprint());
                return true;
            }
        }
        return false;
    }
}