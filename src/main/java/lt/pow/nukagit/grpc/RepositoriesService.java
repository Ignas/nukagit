package lt.pow.nukagit.grpc;

import lt.pow.nukagit.proto.RepositoriesServiceGrpc;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RepositoriesService extends RepositoriesServiceGrpc.RepositoriesServiceImplBase {

  @Inject
  public RepositoriesService() {}
}
