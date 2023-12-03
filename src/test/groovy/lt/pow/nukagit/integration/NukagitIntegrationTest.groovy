package lt.pow.nukagit.integration

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.minio.MakeBucketArgs
import lt.pow.nukagit.DaggerMainComponent
import lt.pow.nukagit.MainComponent
import lt.pow.nukagit.proto.Repositories
import lt.pow.nukagit.proto.RepositoriesServiceGrpc
import org.apache.sshd.client.SshClient
import org.apache.sshd.git.transport.GitSshdSessionFactory
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification
import spock.lang.TempDir

import java.security.KeyPair
import java.security.KeyPairGenerator

@Testcontainers
class NukagitIntegrationTest extends Specification {
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
    RepositoriesServiceGrpc.RepositoriesServiceBlockingStub grpcClient
    int sshPort
    int grpcPort

    @TempDir
    File testDir

    def getRandomPort() {
        ServerSocket serverSocket = new ServerSocket(0)
        serverSocket.close()
        return serverSocket.localPort
    }

    def setup() {
        sshPort = getRandomPort()
        grpcPort = getRandomPort()

        File testConfig = new File(testDir, "config.yaml")
        System.setProperty("nukagit.config_path", testConfig.absolutePath)
        testConfig << """
        ssh:
          port: ${sshPort}
          hostname: localhost
          hostKey: ${testConfig.absolutePath}/ssh_host_key.pem

        grpc:
          port: ${grpcPort}

        database:
          jdbcUrl: jdbc:mysql://localhost:${mysql.getMappedPort(3306)}/nukagit
           
        minio:
          endpoint: http://localhost:${minio.getMappedPort(9000)}
        """

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                .usePlaintext()
                .build()
        grpcClient = RepositoriesServiceGrpc.newBlockingStub(channel)

        component.minio().makeBucket(MakeBucketArgs.builder()
            .bucket("nukagit")
            .build())
        component.migrateEntrypoint().run()

        component.sshServer().start()
        component.grpcServer().start()

        sshClient = SshClient.setUpDefaultClient()
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        keyPair = keyGen.generateKeyPair()
        sshClient.addPublicKeyIdentity(keyPair)

        CredentialsProvider.setDefault(new UsernamePasswordCredentialsProvider("git", "git"))
        SshSessionFactory.setInstance(new GitSshdSessionFactory(sshClient))
    }

    def cleanup() {
        sshClient.stop()
        component.grpcServer().shutdown()
        component.sshServer().stop()
    }

    def cloneRepository(String path) {
        grpcClient.createRepository(Repositories.CreateRepositoryRequest.newBuilder().setRepositoryName(path).build())
        var clonePath = new File(testDir, path.replace("/", "-"))
        CloneCommand cloneCommand = Git.cloneRepository()
        cloneCommand.setURI("ssh://git@localhost:${sshPort}/${path}")
        cloneCommand.setDirectory(clonePath)
        return cloneCommand.call()
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
        git.push().call()
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
        git.push().call()
    }
}