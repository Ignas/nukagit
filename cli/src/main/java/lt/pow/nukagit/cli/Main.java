package lt.pow.nukagit.cli;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lt.pow.nukagit.proto.RepositoriesServiceGrpc;
import lt.pow.nukagit.proto.UsersServiceGrpc;
import picocli.CommandLine;

@CommandLine.Command(name = "nuka_cli", description = "NukaGit CLI client",
        subcommands = {
                AddUser.class,
                CreateRepository.class,
                ListRepositories.class,
        })
public class Main implements Runnable {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    private boolean helpRequested;

    @CommandLine.Option(names = {"-H", "--host"}, description = "host to connect to", defaultValue = "localhost")
    private String host;

    @CommandLine.Option(names = {"-p", "--port"}, description = "port to connect to", defaultValue = "50051")
    private Integer port;

    private ManagedChannel channel;
    RepositoriesServiceGrpc.RepositoriesServiceBlockingStub repositoriesGrpcClient;
    UsersServiceGrpc.UsersServiceBlockingStub usersGrpcClient;

    ManagedChannel getChannel() {
        if (channel == null) {
            channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
        }
        return channel;
    }

    public RepositoriesServiceGrpc.RepositoriesServiceBlockingStub repositoriesGrpcClient() {
        if (repositoriesGrpcClient == null) {
            repositoriesGrpcClient = RepositoriesServiceGrpc.newBlockingStub(getChannel());
        }
        return repositoriesGrpcClient;
    }

    public UsersServiceGrpc.UsersServiceBlockingStub usersGrpcClient() {
        if (usersGrpcClient == null) {
            usersGrpcClient = UsersServiceGrpc.newBlockingStub(getChannel());
        }
        return usersGrpcClient;
    }

    @Override
    public void run() {
        CommandLine cmd = new CommandLine(this);
        // This method is called if no subcommands are specified
        if (helpRequested || cmd.getUnmatchedArguments().isEmpty()) {
            cmd.usage(System.out);
        } else {
            System.out.println("No valid subcommands provided. Use -h or --help for usage information.");
        }
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Main());
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}