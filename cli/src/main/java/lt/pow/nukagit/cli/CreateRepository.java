package lt.pow.nukagit.cli;

import lt.pow.nukagit.proto.Repositories;
import picocli.CommandLine;

@CommandLine.Command(name = "create_repository", description = "Create repository")
public class CreateRepository implements Runnable {
    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Parameters(index = "0", description = "Repository name.")
    private String repositoryName;

    @Override
    public void run() {
        System.out.println("Creating repository " + repositoryName);
        parent.repositoriesGrpcClient().createRepository(Repositories.CreateRepositoryRequest.newBuilder()
                .setRepositoryName(repositoryName)
                .build());
        System.out.println("Repository created");
    }
}
