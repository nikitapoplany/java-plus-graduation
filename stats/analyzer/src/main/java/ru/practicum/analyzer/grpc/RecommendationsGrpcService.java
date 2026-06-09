package ru.practicum.analyzer.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.analyzer.service.RecommendedEvent;
import ru.practicum.ewm.stats.proto.recommendation.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.recommendation.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.recommendation.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.UserPredictionsRequestProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            recommendationService.getRecommendationsForUser(request.getUserId(), request.getMaxResults())
                    .forEach(event -> responseObserver.onNext(toProto(event)));
            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error("Cannot get recommendations for user {}", request.getUserId(), exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Cannot get recommendations")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            recommendationService.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults())
                    .forEach(event -> responseObserver.onNext(toProto(event)));
            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error("Cannot get similar events for event {}", request.getEventId(), exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Cannot get similar events")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            recommendationService.getInteractionsCount(request.getEventIdList())
                    .forEach(event -> responseObserver.onNext(toProto(event)));
            responseObserver.onCompleted();
        } catch (Exception exception) {
            log.error("Cannot get interactions count", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Cannot get interactions count")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private RecommendedEventProto toProto(RecommendedEvent event) {
        return RecommendedEventProto.newBuilder()
                .setEventId(event.eventId())
                .setScore(event.score())
                .build();
    }
}
