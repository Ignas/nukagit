package lt.pow.nukagit.db.repositories

import lt.pow.nukagit.db.DatabaseTestBase
import lt.pow.nukagit.db.dao.PublicKeysDao
import lt.pow.nukagit.db.dao.UsersDao
import lt.pow.nukagit.db.entities.PublicKeyData
import org.jeasy.random.EasyRandom
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

@Testcontainers
class PublicKeyRepositoryDaoTest extends DatabaseTestBase {
    @Shared
    EasyRandom random = new EasyRandom()
    PublicKeyRepository repository
    PublicKeyDecoder publicKeyDecoder

    def "you can add a user with public key"() {
        given:
        def username = random.nextObject(String.class)
        def publicKey = random.nextObject(String.class)
        def testKeyData = repository.generateRandomPublicKeyData()
        publicKeyDecoder.decodePublicKey(publicKey) >> testKeyData
        when:
        repository.addUserWithKey(username, publicKey)
        then:
        repository.publicKeyCollection.size() == 1
        def userKey = repository.publicKeyCollection[0]
        userKey.username() == username
        userKey.publicKeyData() == testKeyData
    }

    void setup() {
        // Create DAO
        def publicKeysDao = jdbi.onDemand(PublicKeysDao.class)
        def usersDao = jdbi.onDemand(UsersDao.class)
        def usernameValidator = Mock(UsernameValidator.class)
        usernameValidator.isValid(_) >> true
        publicKeyDecoder = Mock(PublicKeyDecoder.class)
        // Create repository
        repository = new PublicKeyRepository(usersDao, publicKeysDao, usernameValidator, publicKeyDecoder)
    }

}
