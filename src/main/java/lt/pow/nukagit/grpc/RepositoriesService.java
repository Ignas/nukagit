package lt.pow.nukagit.grpc;

import io.grpc.stub.StreamObserver;
import javax.inject.Inject;
import javax.inject.Singleton;
import lt.pow.nukagit.dfs.DfsRepositoryResolver;
import lt.pow.nukagit.proto.Repositories;
import lt.pow.nukagit.proto.RepositoriesServiceGrpc;
import lt.pow.nukagit.proto.Types;

@Singleton
public class RepositoriesService extends RepositoriesServiceGrpc.RepositoriesServiceImplBase {

  private final DfsRepositoryResolver repositoryResolver;

  @Inject
  public RepositoriesService(DfsRepositoryResolver repositoryResolver) {
    this.repositoryResolver = repositoryResolver;
  }

  @Override
  public void listRepositories(
      Repositories.ListRepositoriesRequest request,
      StreamObserver<Repositories.ListRepositoriesResponse> responseObserver) {
    Repositories.ListRepositoriesResponse response =
        Repositories.ListRepositoriesResponse.newBuilder()
            .addAllRepositories(
                repositoryResolver.listRepositories().stream()
                    .map(
                        repositoryName ->
                            Types.Repository.newBuilder().setName(repositoryName).build())
                    .toList())
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
