package ru.practicum.collector.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.collector.config.CollectorKafkaProperties;
import ru.practicum.ewm.stats.avro.AvroBytes;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionCollectorGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final CollectorKafkaProperties properties;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            validate(request);
            UserActionAvro avro = UserActionMapper.toAvro(request);
            kafkaTemplate.send(
                    properties.getTopics().getUserActions(),
                    AvroBytes.serialize(avro)
            );
            log.debug("Collected user action: userId={}, eventId={}, type={}",
                    avro.getUserId(), avro.getEventId(), avro.getActionType());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Cannot collect user action", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Cannot collect user action")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private void validate(UserActionProto request) {
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("user_id must be positive");
        }
        if (request.getEventId() <= 0) {
            throw new IllegalArgumentException("event_id must be positive");
        }
    }
}
