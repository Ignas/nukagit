package lt.pow.nukagit.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class GrpcHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcHelpers.class);

    public static <RQ, RS> void unaryCall(RQ request, StreamObserver<RS> responseObserver, Function<RQ, RS> method) {
        RS response;
        try {
            response = method.apply(request);
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
            return;
        } catch (Exception e) {
            LOGGER.error("Error processing request", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
