package ru.practicum.statsclient.recommendation;

import net.devh.boot.grpc.client.inject.GrpcClient;
import ru.practicum.ewm.stats.proto.recommendation.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.recommendation.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.recommendation.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.recommendation.UserPredictionsRequestProto;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GrpcRecommendationsClient implements RecommendationsClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    @Override
    public List<RecommendedEventScore> getRecommendationsForUser(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return asStream(client.getRecommendationsForUser(request))
                .map(this::toScore)
                .toList();
    }

    @Override
    public List<RecommendedEventScore> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        return asStream(client.getSimilarEvents(request))
                .map(this::toScore)
                .toList();
    }

    @Override
    public List<RecommendedEventScore> getInteractionsCount(Collection<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds == null ? List.of() : eventIds)
                .build();
        return asStream(client.getInteractionsCount(request))
                .map(this::toScore)
                .toList();
    }

    private RecommendedEventScore toScore(RecommendedEventProto proto) {
        return new RecommendedEventScore(proto.getEventId(), proto.getScore());
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
