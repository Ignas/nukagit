package lt.pow.nukagit.db.repositories


import org.yaml.snakeyaml.Yaml
import spock.lang.Shared
import spock.lang.Specification

import java.security.spec.X509EncodedKeySpec

class PublicKeyDecoderTest extends Specification {
    @Shared
    List<String> validKeys

    @Shared
    List<String> invalidKeys

    PublicKeyDecoder decoder = new PublicKeyDecoder()

    def setupSpec() {
        def keyFile = new File(getClass().getResource('/fixtures/ssh_keys.yaml').toURI())
        Map<String, List<String>> yaml = new Yaml().load(keyFile.newReader())
        validKeys = yaml.get("valid")
        invalidKeys = yaml.get("invalid")
    }

    def "test valid keys"() {
        expect:
        decoder.decodePublicKey(key).key() != null
        where:
        key << validKeys
    }

    def "test invalid keys"() {
        when:
        decoder.decodePublicKey(key)
        then:
        thrown(InvalidKeyStringException)
        where:
        key << invalidKeys
    }
}
