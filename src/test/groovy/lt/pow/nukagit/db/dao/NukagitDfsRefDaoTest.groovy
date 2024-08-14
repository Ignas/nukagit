package lt.pow.nukagit.db.dao


import lt.pow.nukagit.db.DatabaseTestBase
import lt.pow.nukagit.db.entities.ImmutableDfsRef
import org.jeasy.random.EasyRandom
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared


@Testcontainers
class NukagitDfsRefDaoTest extends DatabaseTestBase {
    @Shared
    EasyRandom random = new EasyRandom()
    NukagitDfsRepositoryDao repoDao
    NukagitDfsRefDao dao

    def "you can create new refs"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        expect:
        dao.create(repoId, ref) == 1
        dao.listRefs(repoId).contains(ref)
    }

    def "trying to create a ref if one already exists with the same name will not do anything"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        def ref2 = random.nextObject(ImmutableDfsRef).withName(ref.name())
        expect:
        dao.create(repoId, ref) == 1
        dao.create(repoId, ref2) == 0
        dao.listRefs(repoId).size() == 1
        dao.listRefs(repoId).contains(ref)
    }

    def "you can list refs"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref1 = random.nextObject(ImmutableDfsRef)
        def ref2 = random.nextObject(ImmutableDfsRef)
        dao.create(repoId, ref1)
        dao.create(repoId, ref2)
        expect:
        dao.listRefs(repoId).containsAll([ref1, ref2])
    }

    def "you can remove refs"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        dao.create(repoId, ref)
        expect:
        dao.compareAndRemove(repoId, ref) == 1
        dao.listRefs(repoId).size() == 0
    }

    def "if any of the attributes of the ref are different it will not be removed"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        dao.create(repoId, ref)
        def ref2 = ref.withObjectID(random.nextObject(String))
        expect:
        dao.compareAndRemove(repoId, ref2) == 0
        dao.listRefs(repoId).size() == 1
    }

    def "if the ref does not exist it will not be removed"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        expect:
        dao.compareAndRemove(repoId, ref) == 0
        dao.listRefs(repoId).size() == 0
    }

    def "if object id is null or target is null removal still removes the ref"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
                .withObjectID(null)
                .withTarget(null)
        dao.create(repoId, ref)
        expect:
        dao.compareAndRemove(repoId, ref) == 1
        dao.listRefs(repoId).size() == 0
    }

    def "peel status is ignored when matching old ref in removal calls"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        dao.create(repoId, ref)
        def ref2 = ref
                .withIsPeeled(!ref.isPeeled())
                .withPeeledRef(random.nextObject(String))
        expect:
        dao.compareAndRemove(repoId, ref2) == 1
        dao.listRefs(repoId).size() == 0
    }

    def "refs can be updated"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        dao.create(repoId, ref)
        def ref2 = random.nextObject(ImmutableDfsRef).withName(ref.name())
        expect:
        dao.compareAndPut(repoId, ref, ref2) == 1
        dao.listRefs(repoId) == [ref2]
    }

    def "refs with null object id or target can be updated"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
                .withObjectID(null)
                .withTarget(null)
        dao.create(repoId, ref)
        def ref2 = random.nextObject(ImmutableDfsRef)
                .withName(ref.name())
        expect:
        dao.compareAndPut(repoId, ref, ref2) == 1
        dao.listRefs(repoId) == [ref2]
    }

    def "peeled status is ignored when matching old ref in update calls"() {
        given:
        def repoId = repoDao.upsertRepositoryAndGetId(random.nextObject(String))
        def ref = random.nextObject(ImmutableDfsRef)
        dao.create(repoId, ref)
        def ref2 = ref
                .withIsPeeled(!ref.isPeeled())
                .withPeeledRef(random.nextObject(String))
        def ref3 = random.nextObject(ImmutableDfsRef).withName(ref.name())
        expect:
        dao.compareAndPut(repoId, ref2, ref3) == 1
        dao.listRefs(repoId) == [ref3]
    }

    void setup() {
        // Create DAOs
        repoDao = jdbi.onDemand(NukagitDfsRepositoryDao.class)
        dao = jdbi.onDemand(NukagitDfsRefDao.class)
    }
}
