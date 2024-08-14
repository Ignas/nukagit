package lt.pow.nukagit.grpc;

import io.grpc.stub.StreamObserver;

import javax.inject.Inject;
import javax.inject.Singleton;

import lt.pow.nukagit.db.dao.NukagitDfsObjDao;
import lt.pow.nukagit.db.dao.NukagitDfsRepositoryDao;
import lt.pow.nukagit.dfs.DfsRepositoryResolver;
import lt.pow.nukagit.proto.Repositories;
import lt.pow.nukagit.proto.RepositoriesServiceGrpc;
import lt.pow.nukagit.proto.Types;

@Singleton
public class RepositoriesService extends RepositoriesServiceGrpc.RepositoriesServiceImplBase {

    private final DfsRepositoryResolver repositoryResolver;

    private final NukagitDfsRepositoryDao nukagitDfsRepositoryDao;

    @Inject
    public RepositoriesService(DfsRepositoryResolver repositoryResolver, NukagitDfsRepositoryDao nukagitDfsRepositoryDao) {
        this.repositoryResolver = repositoryResolver;
        this.nukagitDfsRepositoryDao = nukagitDfsRepositoryDao;
    }

    private Repositories.ListRepositoriesResponse listRepositories(Repositories.ListRepositoriesRequest request) {
        return Repositories.ListRepositoriesResponse.newBuilder()
                .addAllRepositories(
                        repositoryResolver.listRepositories().stream()
                                .map(
                                        repositoryName ->
                                                Types.Repository.newBuilder().setName(repositoryName).build())
                                .toList())
                .build();
    }

    @Override
    public void listRepositories(
            Repositories.ListRepositoriesRequest request,
            StreamObserver<Repositories.ListRepositoriesResponse> responseObserver) {
        GrpcHelpers.unaryCall(request, responseObserver, this::listRepositories);
    }

    private Repositories.CreateRepositoryResponse createRepository(Repositories.CreateRepositoryRequest request) {
        var repositoryName = request.getRepositoryName();
        if (!repositoryName.startsWith("/")) {
            repositoryName = "/" + repositoryName;
        }
        nukagitDfsRepositoryDao.upsertRepository(repositoryName);
        return Repositories.CreateRepositoryResponse.newBuilder().build();
    }

    @Override
    public void createRepository(Repositories.CreateRepositoryRequest request, StreamObserver<Repositories.CreateRepositoryResponse> responseObserver) {
        GrpcHelpers.unaryCall(request, responseObserver, this::createRepository);
    }
}
