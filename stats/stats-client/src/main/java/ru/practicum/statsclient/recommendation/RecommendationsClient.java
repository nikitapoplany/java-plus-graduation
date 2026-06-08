package ru.practicum.statsclient.recommendation;

import java.util.Collection;
import java.util.List;

public interface RecommendationsClient {

    List<RecommendedEventScore> getRecommendationsForUser(long userId, int maxResults);

    List<RecommendedEventScore> getSimilarEvents(long eventId, long userId, int maxResults);

    List<RecommendedEventScore> getInteractionsCount(Collection<Long> eventIds);
}
