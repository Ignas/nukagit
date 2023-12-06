package lt.pow.nukagit.db.dao;

import lt.pow.nukagit.db.entities.PublicKey;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

public interface PublicKeysDao {
    @SqlQuery("SELECT public_keys.*, u.username FROM public_keys" +
            " LEFT JOIN users AS u ON u.id = user_id" +
            " WHERE public_keys.not_archived = true")
    List<PublicKey> listPublicKeys();

    @SqlQuery("SELECT public_keys.*, u.username FROM public_keys" +
            " LEFT JOIN users AS u ON u.id = user_id" +
            " WHERE public_keys.not_archived = true AND user_id = :userId")
    List<PublicKey> listPublicKeysForUser(@Bind("userId") UUID userId);
    @SqlUpdate("INSERT INTO public_keys (id, user_id, fingerprint, public_key)" +
            " VALUES (UUID(), :userId, :fingerprint, :publicKey)")
    void addPublicKey(@Bind("userId") UUID userId, @Bind("fingerprint") String fingerprint, @Bind("publicKey") String publicKey);

    @SqlUpdate("UPDATE public_keys SET deleted_on = NOW() WHERE id = :id")
    void removePublicKey(@Bind("id") UUID id);

    @SqlUpdate("UPDATE public_keys SET deleted_on = NOW() WHERE fingerprint = :fingerprint")
    void removePublicKeyByFingerprint(@Bind("fingerprint") String fingerprint);
}
