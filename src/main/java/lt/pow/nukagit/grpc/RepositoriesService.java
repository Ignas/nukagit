package lt.pow.nukagit.grpc;

import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import javax.inject.Singleton;

import lt.pow.nukagit.db.dao.NukagitDfsDao;
import lt.pow.nukagit.dfs.DfsRepositoryResolver;
import lt.pow.nukagit.proto.Repositories;
import lt.pow.nukagit.proto.RepositoriesServiceGrpc;
import lt.pow.nukagit.proto.Types;

@Singleton
public class RepositoriesService extends RepositoriesServiceGrpc.RepositoriesServiceImplBase {

    private final DfsRepositoryResolver repositoryResolver;
    private final NukagitDfsDao nukagitDfsDao;

    @Inject
    public RepositoriesService(DfsRepositoryResolver repositoryResolver, NukagitDfsDao nukagitDfsDao) {
        this.repositoryResolver = repositoryResolver;
        this.nukagitDfsDao = nukagitDfsDao;
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

    @Override
    public void createRepository(Repositories.CreateRepositoryRequest request, StreamObserver<Repositories.CreateRepositoryResponse> responseObserver) {
        var repositoryName = request.getRepositoryName();
        if (!repositoryName.startsWith("/")) {
            repositoryName = "/" + repositoryName;
        }
        nukagitDfsDao.upsertRepository(repositoryName);
        Repositories.CreateRepositoryResponse response = Repositories.CreateRepositoryResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
