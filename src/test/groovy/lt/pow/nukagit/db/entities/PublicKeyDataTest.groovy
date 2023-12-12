package lt.pow.nukagit.db.entities

import lt.pow.nukagit.db.repositories.PublicKeyDecoder
import org.yaml.snakeyaml.Yaml
import spock.lang.Shared
import spock.lang.Specification

class PublicKeyDataTest extends Specification {
    PublicKeyDecoder decoder = new PublicKeyDecoder()

    @Shared
    List<Map<String, String>> keys

    def setupSpec() {
        def keyFile = new File(getClass().getResource('/fixtures/fingerprints.yaml').toURI())
        keys = new Yaml().load(keyFile.newReader())
    }

    def "test valid keys"() {
        expect:
        decoder.decodePublicKey(key["key"]).fingerprint() == key["myfingerprint"]
        where:
        key << keys
    }
}
