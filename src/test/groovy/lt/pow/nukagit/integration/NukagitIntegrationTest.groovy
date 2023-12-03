package lt.pow.nukagit.integration

import io.minio.MakeBucketArgs
import lt.pow.nukagit.DaggerMainComponent
import lt.pow.nukagit.MainComponent
import org.apache.sshd.client.SshClient
import org.apache.sshd.git.transport.GitSshdSessionFactory
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.security.KeyPair
import java.security.KeyPairGenerator

@Testcontainers
class NukagitIntegrationTest extends Specification {
    @Shared
    MySQLContainer mysql = new MySQLContainer("mysql:8")
            .withDatabaseName("nukagit")
            .withUsername("nukagit")
            .withPassword("password")

    GenericContainer minio = new GenericContainer(DockerImageName.parse("quay.io/minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", "minio99")
            .withEnv("MINIO_ROOT_PASSWORD", "minio123")
            .withExposedPorts(9000)
            .withCommand("server /data --console-address :9001")

    MainComponent component = DaggerMainComponent.create()
    SshClient sshClient
    KeyPair keyPair

    @TempDir
    File testDir

    def setup() {
        File testConfig = new File(testDir, "config.yaml")
        System.setProperty("nukagit.config_path", testConfig.absolutePath)
        testConfig << """
        ssh:
          port: 2222
          hostname: localhost
          hostKey: ${testConfig.absolutePath}/ssh_host_key.pem

        database:
          jdbcUrl: jdbc:mysql://localhost:${mysql.getMappedPort(3306)}/nukagit
           
        minio:
          endpoint: http://localhost:${minio.getMappedPort(9000)}
        """
        component.minio().makeBucket(MakeBucketArgs.builder()
            .bucket("nukagit")
            .build())
        component.migrateEntrypoint().run()

        component.sshServer().start()

        sshClient = SshClient.setUpDefaultClient()
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        keyPair = keyGen.generateKeyPair()
        sshClient.addPublicKeyIdentity(keyPair)
    }

    def cleanup() {
        sshClient.stop()
        component.sshServer().stop()
    }

    def cloneRepository(String path) {
        var clonePath = new File(testDir, path.replace("/", "-"))
        CloneCommand cloneCommand = Git.cloneRepository()
        cloneCommand.setURI("ssh://git@localhost:2222/" + path)
        cloneCommand.setDirectory(clonePath)
        return callGit(cloneCommand)
    }

    <R> R callGit(TransportCommand<? extends GitCommand, R> command) {
        command.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", "git"))
                .setTransportConfigCallback {
                    ((SshTransport) it).setSshSessionFactory(new GitSshdSessionFactory(sshClient))}
        return command.call()
    }

    def "test clone empty in-memory repo add file and push it back"() {
        given:
        var git = cloneRepository("memory/repo")
        when:
        var newFile = new File(git.repository.directory, "test.txt")
        newFile.write("Test Content")
        then:
        git.add().addFilepattern(".").call()
        git.commit().setAuthor("test", "test@example.com").setMessage("Test Change").call()
        callGit(git.push())
    }

    def "test clone minio backed repo add file and push it back"() {
        given:
        var git = cloneRepository("minio/repo")
        when:
        var newFile = new File(git.repository.directory, "test.txt")
        newFile.write("Test Content")
        then:
        git.add().addFilepattern(".").call()
        git.commit().setAuthor("test", "test@example.com").setMessage("Test Change").call()
        callGit(git.push())
    }
}