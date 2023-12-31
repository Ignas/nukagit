package lt.pow.nukagit.db.dao


import lt.pow.nukagit.db.DatabaseTestBase
import lt.pow.nukagit.db.entities.ImmutablePack
import org.jeasy.random.EasyRandom
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared


@Testcontainers
class NukagitDfsDaoTest extends DatabaseTestBase {
    @Shared
    EasyRandom random = new EasyRandom()
    NukagitDfsDao dao

    def "upsert repository returns repository id"() {
        expect:
        dao.upsertRepositoryAndGetId("repo") != null
    }

    def "next repository gets a new id"() {
        given:
        def repoId1 = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        def repoId2 = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        expect:
        repoId1 != repoId2
    }

    def "same repository gets a same id"() {
        given:
        def repoName = random.nextObject(String.class)
        def repoId1 = dao.upsertRepositoryAndGetId(repoName)
        def repoId2 = dao.upsertRepositoryAndGetId(repoName)
        expect:
        repoId1 == repoId2
    }

    def "if we archive repository we get a new id"() {
        given:
        def repoName = random.nextObject(String.class)
        def repoId1 = dao.upsertRepositoryAndGetId(repoName)
        dao.archiveRepository(repoId1)
        def repoId2 = dao.upsertRepositoryAndGetId(repoName)
        expect:
        repoId1 != repoId2
    }

    def "we can commit an empty set of packs"() {
        given:
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        when:
        dao.commitPack(repoId, [], [])
        then:
        dao.listPacks(repoId).isEmpty()
    }

    def "We can add new packs when committing"() {
        given:
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        def packs = random.objects(ImmutablePack, 3).toList()
        when:
        dao.commitPack(repoId, packs, [])
        then:
        dao.listPacks(repoId) as Set == packs as Set
    }

    def "Packs from previous commit are kept"() {
        given:
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        def packs = random.objects(ImmutablePack, 3).toList()
        when:
        dao.commitPack(repoId, packs, [])
        dao.commitPack(repoId, [], [])
        then:
        dao.listPacks(repoId) as Set == packs as Set
    }

    def "Packs from previous commit are kept, unless they are replaced"() {
        given:
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        def packs = random.objects(ImmutablePack, 3).toList()
        when:
        dao.commitPack(repoId, packs, [])
        dao.commitPack(repoId, [], [packs[0]])
        then:
        dao.listPacks(repoId) as Set == packs.drop(1) as Set
    }

    def "2 ref packs with same max update index conflict"() {
        given:
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        def pack1 = random.nextObject(ImmutablePack).withMaxUpdateIndex(15).withExt("ref")
        def pack2 = random.nextObject(ImmutablePack).withMaxUpdateIndex(15).withExt("ref")
        dao.commitPack(repoId, [pack1], [])
        when:
        dao.commitPack(repoId, [pack2], [])
        then:
        thrown(NukagitDfsPackConflictException)
    }

    def "2 non-ref packs with same max update index don't conflict"() {
        given:
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        def pack1 = random.nextObject(ImmutablePack).withMaxUpdateIndex(15).withExt("idx")
        def pack2 = random.nextObject(ImmutablePack).withMaxUpdateIndex(15).withExt("ref")
        when:
        dao.commitPack(repoId, [pack1], [])
        dao.commitPack(repoId, [pack2], [])
        then:
        notThrown(NukagitDfsPackConflictException)
    }

    void setup() {
        // Create DAO
        dao = jdbi.onDemand(NukagitDfsDao.class)
    }
}
