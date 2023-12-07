package lt.pow.nukagit.grpc;

import lt.pow.nukagit.proto.UsersServiceGrpc;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UsersService extends UsersServiceGrpc.UsersServiceImplBase {
    @Inject
    public UsersService() {
    }
}
