package lt.pow.nukagit.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.function.Function;

public class GrpcHelpers {
    public static <RQ, RS> void unaryCall(RQ request, StreamObserver<RS> responseObserver, Function<RQ, RS> method) {
        RS response;
        try {
            response = method.apply(request);
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
            return;
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
