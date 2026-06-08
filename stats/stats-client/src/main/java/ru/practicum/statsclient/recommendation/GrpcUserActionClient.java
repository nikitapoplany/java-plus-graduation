package ru.practicum.statsclient.recommendation;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;

import java.time.Instant;

public class GrpcUserActionClient implements UserActionClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    @Override
    public void collect(long userId, long eventId, UserActionType actionType, Instant timestamp) {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(toProto(actionType))
                .setTimestamp(toProto(timestamp))
                .build();
        Empty ignored = client.collectUserAction(request);
    }

    private ActionTypeProto toProto(UserActionType actionType) {
        return switch (actionType) {
            case VIEW -> ActionTypeProto.ACTION_VIEW;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }

    private Timestamp toProto(Instant instant) {
        Instant value = instant != null ? instant : Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }
}
