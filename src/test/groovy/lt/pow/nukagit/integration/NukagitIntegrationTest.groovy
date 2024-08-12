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
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.jeasy.random.EasyRandom
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.FailsWith
import spock.lang.Specification
import spock.lang.TempDir
import org.spockframework.runtime.ConditionNotSatisfiedError

import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
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
    var random = new EasyRandom()
    SshClient sshClient
    KeyPair keyPair

    ManagedChannel grpcChannel
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

        grpcChannel = ManagedChannelBuilder.forAddress("localhost", grpcPort)
                .usePlaintext()
                .build()
        repositoriesGrpcClient = RepositoriesServiceGrpc.newBlockingStub(grpcChannel)
        usersGrpcClient = UsersServiceGrpc.newBlockingStub(grpcChannel)

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
        grpcChannel.shutdownNow()
        grpcChannel.awaitTermination(1, TimeUnit.SECONDS)
        component.grpcServer().shutdownNow()
        component.grpcServer().awaitTermination(1, TimeUnit.SECONDS)
        component.sshServer().stop()
    }

    def createRepository(String path) {
        repositoriesGrpcClient.createRepository(Repositories.CreateRepositoryRequest.newBuilder().setRepositoryName(path).build())
        var randomPath = random.nextObject(String)
        Git git = Git.init()
                .setDirectory(new File(testDir, randomPath))
                .setInitialBranch("main")
                .call()
        git.remoteAdd()
                .setName("origin")
                .setUri(new URIish("ssh://git@localhost:${sshPort}/${path}"))
                .call()
        commitRandomFile(git)
        git.checkout().setName("main").call()
        git.push().setPushAll().call()
        // This does not set HEAD, might be a bug in the server
        return git
    }

    def cloneRepository(String path) {
        var randomPath = UUID.randomUUID().toString()
        var clonePath = new File(testDir, randomPath)
        CloneCommand cloneCommand = Git.cloneRepository()
        cloneCommand.setURI("ssh://git@localhost:${sshPort}/${path}")
        cloneCommand.setDirectory(clonePath)
        // For now set it to main explicitly, because HEAD does not exist in the remote repository
        cloneCommand.setBranch("main")
        return cloneCommand.call()
    }

    def commitRandomFile(Git git) {
        var newFile = new File(git.repository.workTree, "test.txt")
        newFile.write(random.nextObject(String.class))
        git.add().addFilepattern(".").call()
        git.commit().setAuthor("test", "test@example.com").setMessage("Test Change").call()
    }

    def "test clone empty in-memory repo add file and push it back"() {
        given:
        createRepository("memory/repo")
        var git = cloneRepository("memory/repo")
        when:
        commitRandomFile(git)
        then:
        git.push().call()
    }

    def "test clone minio backed repo add file and push it back"() {
        given:
        createRepository("minio/repo")
        var git = cloneRepository("minio/repo")
        when:
        commitRandomFile(git)
        then:
        git.push().call()
    }

    def "test pushing conflicting changes to main should fail"() {
        given:
        var repoName = "minio/repo"
        createRepository(repoName)
        var git1 = cloneRepository(repoName)
        var git2 = cloneRepository(repoName)
        commitRandomFile(git1)
        commitRandomFile(git2)
        when:
        var pushResult1 = git1.push().call().asList()
        var pushResult2 = git2.push().call().asList()
        then:
        pushResult1.size() == 1
        pushResult2.size() == 1
        pushResult1.get(0).getRemoteUpdate("refs/heads/main").getStatus() == RemoteRefUpdate.Status.OK
        pushResult2.get(0).getRemoteUpdate("refs/heads/main").getStatus() == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD
    }

    @FailsWith(ConditionNotSatisfiedError)
    def "test concurrent pushes to different branches should conflict and fail"() {
        // For now this is a conflict causing situation, but I intend to implement
        // a mysql native reftable that will handle individual branch updates
        given:
        createRepository("minio/repo")

        var nThreads = 10
        ArrayList<Git> repositories = []
        nThreads.times {
            var git = cloneRepository("minio/repo")
            commitRandomFile(git)
            repositories.push(git)
        }
        when:
        // concurrently run push
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads)
        List<Future<RemoteRefUpdate.Status>> futures = []
        repositories.forEach {git ->
            def closure = {
                var branchName = UUID.randomUUID().toString()
                var result = git.push()
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec("main:${branchName}"))
                        .call()
                        .first()
                        .getRemoteUpdates()
                        .first()
                        .getStatus()
                return result
            }
            Future<RemoteRefUpdate.Status> future = executorService.submit(closure as Callable<RemoteRefUpdate.Status>)
            futures.add(future)
        }
        executorService.shutdown()
        executorService.awaitTermination(1, TimeUnit.MINUTES)
        then:

        futures.each { future ->
            // This statement fails because push gets REJECTED_OTHER_REASON status
            assert future.get() == RemoteRefUpdate.Status.OK
        }

        var git = cloneRepository("minio/repo")
        // This statement fails because not all branches have been pushed successfully
        git.lsRemote().call().size() == nThreads + 1
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