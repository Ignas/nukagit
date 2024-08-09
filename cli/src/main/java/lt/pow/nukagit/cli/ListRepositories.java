package lt.pow.nukagit.cli;

import lt.pow.nukagit.proto.Repositories;
import picocli.CommandLine;

@CommandLine.Command(name = "list_repositories", description = "List repositories")
public class ListRepositories implements Runnable {
    @CommandLine.ParentCommand
    private Main parent;
    @Override
    public void run() {
        System.out.println("Repositories:");
        var repositories = parent.repositoriesGrpcClient()
                .listRepositories(Repositories.ListRepositoriesRequest.newBuilder().build())
                .getRepositoriesList();
        for (var repository : repositories) {
            System.out.println(repository.getName());
        }
    }
}
