package lt.pow.nukagit.db.dao;

import lt.pow.nukagit.db.entities.UserPublicKey;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public interface PublicKeysDao {
    @SqlQuery("SELECT public_keys.*, u.username FROM public_keys" +
            " LEFT JOIN users AS u ON u.id = user_id" +
            " WHERE public_keys.not_archived = true")
    List<UserPublicKey> listPublicKeys();

    @SqlQuery("SELECT public_keys.*, u.username FROM public_keys" +
            " LEFT JOIN users AS u ON u.id = user_id" +
            " WHERE public_keys.not_archived = true AND user_id = :userId")
    List<UserPublicKey> listPublicKeysForUser(@Bind("userId") UUID userId);
    @SqlUpdate("INSERT INTO public_keys (id, user_id, fingerprint, exponent, modulus)" +
            " VALUES (UUID(), :userId, :fingerprint, :exponent, :modulus)")
    void addPublicKey(@Bind("userId") UUID userId, @Bind("fingerprint") String fingerprint, @Bind("exponent") BigInteger exponent, @Bind("modulus") BigInteger modulus);

    @SqlUpdate("UPDATE public_keys SET deleted_on = NOW() WHERE id = :id")
    void removePublicKey(@Bind("id") UUID id);

    @SqlUpdate("UPDATE public_keys SET deleted_on = NOW() WHERE fingerprint = :fingerprint")
    void removePublicKeyByFingerprint(@Bind("fingerprint") String fingerprint);
}
