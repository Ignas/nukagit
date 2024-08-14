package lt.pow.nukagit.db.dao


import lt.pow.nukagit.db.DatabaseTestBase
import org.jeasy.random.EasyRandom
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared


@Testcontainers
class NukagitDfsRepositoryDaoTest extends DatabaseTestBase {
    @Shared
    EasyRandom random = new EasyRandom()
    NukagitDfsRepositoryDao dao

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

    def "we can list non archived repositories"() {
        given:
        def repoName1 = random.nextObject(String.class)
        def repoName2 = random.nextObject(String.class)
        dao.upsertRepository(repoName1)
        dao.upsertRepository(repoName2)
        def repoId = dao.upsertRepositoryAndGetId(random.nextObject(String.class))
        dao.archiveRepository(repoId)
        when:
        def repositoryNames = dao.listRepositories() .stream().map(it -> it.name()).toList()
        then:
        repositoryNames.size() == 2
        repositoryNames == [repoName1, repoName2].sort { it }
    }

    void setup() {
        // Create DAO
        dao = jdbi.onDemand(NukagitDfsRepositoryDao.class)
    }
}
