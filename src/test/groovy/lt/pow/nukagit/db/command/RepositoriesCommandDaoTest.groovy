package lt.pow.nukagit.db.command


import lt.pow.nukagit.db.DatabaseTestBase
import org.testcontainers.spock.Testcontainers

@Testcontainers
class RepositoriesCommandDaoTest extends DatabaseTestBase {
    def repositoriesCommandDao

    def "upsert repository returns repository id"() {
        expect:
        repositoriesCommandDao.upsertAndGetId("repo", "Description") != 0
    }

    def "next repository gets a new id"() {
        given:
        def repoId1 = repositoriesCommandDao.upsertAndGetId("repo1", "Description")
        def repoId2 = repositoriesCommandDao.upsertAndGetId("repo2", "Description")
        expect:
        repoId1 != repoId2
    }

    def "same repository gets a same id"() {
        given:
        def repoId1 = repositoriesCommandDao.upsertAndGetId("repo", "Description")
        def repoId2 = repositoriesCommandDao.upsertAndGetId("repo", "Description")
        expect:
        repoId1 == repoId2
    }

    void setup() {
        // Create DAO
        repositoriesCommandDao = jdbi.onDemand(RepositoriesCommandDao.class)
    }
}
