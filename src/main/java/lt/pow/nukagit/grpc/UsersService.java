package lt.pow.nukagit.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lt.pow.nukagit.db.repositories.InvalidKeyStringException;
import lt.pow.nukagit.db.repositories.InvalidUsernameException;
import lt.pow.nukagit.db.repositories.PublicKeyRepository;
import lt.pow.nukagit.proto.Users;
import lt.pow.nukagit.proto.UsersServiceGrpc;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UsersService extends UsersServiceGrpc.UsersServiceImplBase {
    private final PublicKeyRepository publicKeyRepository;

    @Inject
    public UsersService(PublicKeyRepository publicKeyRepository) {
        this.publicKeyRepository = publicKeyRepository;
    }

    public Users.CreateUserResponse createUser(Users.CreateUserRequest request) {
        var response = Users.CreateUserResponse.newBuilder().build();
        try {
            publicKeyRepository.addUserWithKey(request.getUsername(), request.getPublicKey());
        } catch (InvalidKeyStringException | InvalidUsernameException e) {
            throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
        }
        return response;
    }

    @Override
    public void createUser(Users.CreateUserRequest request, StreamObserver<Users.CreateUserResponse> responseObserver) {
        GrpcHelpers.unaryCall(request, responseObserver, this::createUser);
    }
}
