package lt.pow.nukagit.db.repositories

import lt.pow.nukagit.db.DatabaseTestBase
import lt.pow.nukagit.db.dao.PublicKeysDao
import lt.pow.nukagit.db.dao.UsersDao
import lt.pow.nukagit.db.entities.ImmutablePublicKeyData
import lt.pow.nukagit.db.entities.PublicKeyData
import org.jeasy.random.EasyRandom
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

import java.security.KeyPairGenerator

@Testcontainers
class PublicKeyRepositoryDaoTest extends DatabaseTestBase {
    @Shared
    EasyRandom random = new EasyRandom()
    PublicKeyRepository repository
    PublicKeyDecoder publicKeyDecoder

    def generateRandomRSAKeyData() {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        def keyPair = keyGen.generateKeyPair()
        return ImmutablePublicKeyData.builder()
            .keyType(PublicKeyData.KeyType.RSA)
            .keyBytes(keyPair.public.encoded)
            .build()
    }

    def "you can add a user with public key"() {
        given:
        def username = random.nextObject(String.class)
        def publicKey = random.nextObject(String.class)
        def testKeyData = generateRandomRSAKeyData()
        publicKeyDecoder.decodePublicKey(publicKey) >> testKeyData
        when:
        repository.addUserWithKey(username, publicKey)
        then:
        def keyList = repository.publicKeySupplier.get()
        keyList.size() == 1
        def userKey = keyList[0]
        userKey.username() == username
        userKey.publicKeyData() == testKeyData
    }

    void setup() {
        // Create DAO
        def publicKeysDao = jdbi.onDemand(PublicKeysDao.class)
        def usersDao = jdbi.onDemand(UsersDao.class)
        def usernameValidator = Mock(UsernameValidator.class)
        usernameValidator.isValid(_ as String) >> true
        publicKeyDecoder = Mock(PublicKeyDecoder.class)
        // Create repository
        repository = new PublicKeyRepository(usersDao, publicKeysDao, usernameValidator, publicKeyDecoder)
    }

}
