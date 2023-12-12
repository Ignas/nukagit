package lt.pow.nukagit.db.dao


import lt.pow.nukagit.db.DatabaseTestBase
import lt.pow.nukagit.db.entities.ImmutablePublicKeyData
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.jeasy.random.EasyRandom
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared


@Testcontainers
class PublicKeysDaoTest extends DatabaseTestBase {
    @Shared
    EasyRandom random = new EasyRandom()
    UsersDao usersDao
    PublicKeysDao dao

    def "you can add a public key for a user"() {
        given:
        def username = random.nextObject(String.class)
        def userId = usersDao.upsertUserAndGetId(username)
        def publicKey = random.nextObject(ImmutablePublicKeyData.class)
        when:
        dao.addPublicKey(userId, random.nextObject(String.class), publicKey)
        then:
        dao.listPublicKeys().size() == 1
        def userKey = dao.listPublicKeys()[0]
        userKey.username() == username
        userKey.publicKeyData() == publicKey
        dao.listPublicKeysForUser(userId).size() == 1
    }

    def "listPublicKeys lists all keys in the system"() {
        given:
        def userId1 = usersDao.upsertUserAndGetId(random.nextObject(String.class))
        def userId2 = usersDao.upsertUserAndGetId(random.nextObject(String.class))
        when:
        dao.addPublicKey(userId1, random.nextObject(String.class), random.nextObject(ImmutablePublicKeyData.class))
        dao.addPublicKey(userId1, random.nextObject(String.class), random.nextObject(ImmutablePublicKeyData.class))
        dao.addPublicKey(userId2, random.nextObject(String.class), random.nextObject(ImmutablePublicKeyData.class))
        then:
        dao.listPublicKeys().size() == 3
    }

    def "no 2 keys can share a fingerprint"() {
        given:
        def userId1 = usersDao.upsertUserAndGetId(random.nextObject(String.class))
        def userId2 = usersDao.upsertUserAndGetId(random.nextObject(String.class))
        def fingerprint = random.nextObject(String.class)
        when:
        dao.addPublicKey(userId1, fingerprint, random.nextObject(ImmutablePublicKeyData.class))
        dao.addPublicKey(userId2, fingerprint, random.nextObject(ImmutablePublicKeyData.class))
        then:
        thrown(UnableToExecuteStatementException)
    }

    def "but if you delete a key from one user you should be able to add it to the other one"() {
        given:
        def userId1 = usersDao.upsertUserAndGetId(random.nextObject(String.class))
        def userId2 = usersDao.upsertUserAndGetId(random.nextObject(String.class))
        def fingerprint = random.nextObject(String.class)
        when:
        dao.addPublicKey(userId1, fingerprint, random.nextObject(ImmutablePublicKeyData.class))
        dao.removePublicKeyByFingerprint(fingerprint)
        dao.addPublicKey(userId2, fingerprint, random.nextObject(ImmutablePublicKeyData.class))
        then:
        notThrown(UnableToExecuteStatementException)
    }

    void setup() {
        // Create DAO
        dao = jdbi.onDemand(PublicKeysDao.class)
        usersDao = jdbi.onDemand(UsersDao.class)
    }
}
