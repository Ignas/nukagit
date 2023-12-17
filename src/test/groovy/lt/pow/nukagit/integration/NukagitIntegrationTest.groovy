package lt.pow.nukagit.integration


import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.minio.MakeBucketArgs
import lt.pow.nukagit.DaggerTestComponent
import lt.pow.nukagit.proto.Repositories
import lt.pow.nukagit.proto.RepositoriesServiceGrpc
import lt.pow.nukagit.proto.Users
import lt.pow.nukagit.proto.UsersServiceGrpc
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannel
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.common.channel.Channel
import org.apache.sshd.git.transport.GitSshdSessionFactory
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
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

import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.util.concurrent.TimeUnit

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

    static final String USERNAME = "testuser"
    var component = DaggerTestComponent.create()
    SshClient sshClient
    KeyPair keyPair
    RepositoriesServiceGrpc.RepositoriesServiceBlockingStub repositoriesGrpcClient
    UsersServiceGrpc.UsersServiceBlockingStub usersGrpcClient
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
        repositoriesGrpcClient = RepositoriesServiceGrpc.newBlockingStub(channel)
        usersGrpcClient = UsersServiceGrpc.newBlockingStub(channel)

        component.minio().makeBucket(MakeBucketArgs.builder()
                .bucket("nukagit")
                .build())
        component.migrateEntrypoint().run()

        component.sshServer().start()
        component.grpcServer().start()

        sshClient = SshClient.setUpDefaultClient()

        keyPair = loadKeyPair('/fixtures/id_ecdsa')
        sshClient.addPublicKeyIdentity(keyPair)
        sshClient.start()

        usersGrpcClient.createUser(Users.CreateUserRequest.newBuilder()
                .setUsername(USERNAME)
                .setPublicKey(loadPublicKeyString('/fixtures/id_ecdsa.pub'))
                .build())

        CredentialsProvider.setDefault(new UsernamePasswordCredentialsProvider("git", "git"))
        SshSessionFactory.setInstance(new GitSshdSessionFactory(sshClient))
    }

    private String loadPublicKeyString(String path) {
        new File(getClass().getResource(path).toURI()).text
    }

    def loadKeyPair(String path) {
        def idRsaPem = new File(getClass().getResource(path).toURI())
        def pemParser = new PEMParser(new FileReader(idRsaPem))
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
        PEMKeyPair pemKeyPair = pemParser.readObject() as PEMKeyPair
        return converter.getKeyPair(pemKeyPair)
    }

    def cleanup() {
        sshClient.stop()
        component.grpcServer().shutdownNow()
        component.grpcServer().awaitTermination(1, TimeUnit.SECONDS)
        component.sshServer().stop()
    }

    def cloneRepository(String path) {
        repositoriesGrpcClient.createRepository(Repositories.CreateRepositoryRequest.newBuilder().setRepositoryName(path).build())
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

    def sshRun(String command) {
        def session = sshClient.connect("git", "localhost", sshPort).verify().getSession()
        session.auth().verify()
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream()
        ClientChannel channel = session.createChannel(Channel.CHANNEL_EXEC, command)
        channel.setOut(responseStream)
        channel.setErr(errorStream)
        channel.open().await(1, TimeUnit.SECONDS)
        channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 5000)
        channel.close(false)
        return [responseStream.toString(StandardCharsets.UTF_8), errorStream.toString(StandardCharsets.UTF_8), channel.exitStatus]
    }

    def "test whoami"() {
        when:
        def (out, err, status) = sshRun("whoami")
        then:
        status == 0
        err == ""
        out == "You are: ${USERNAME}\n"
    }

    def "test invalid command"() {
        when:
        def (out, err, status) = sshRun("dummy")
        then:
        status == 1
        err == "Unknown command: dummy\n"
        out == ""
    }
}