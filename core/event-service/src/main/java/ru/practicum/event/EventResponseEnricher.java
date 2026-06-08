package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import ru.practicum.internal.client.RequestServiceClient;
import ru.practicum.statsclient.recommendation.RecommendationsClient;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.validation.ValidationConstants;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventResponseEnricher {

    private final RequestServiceClient requestServiceClient;
    private final ObjectProvider<RecommendationsClient> recommendationsClientProvider;

    public EventDto enrich(EventDto event) {
        if (event == null || event.getId() == null) {
            return event;
        }

        event.setConfirmedRequests(getConfirmedRequests(event.getId()));
        event.setRating(getRating(event.getId()));
        return event;
    }

    public EventShortDto enrich(EventShortDto event) {
        if (event == null || event.getId() == null) {
            return event;
        }

        event.setConfirmedRequests(getConfirmedRequests(event.getId()));
        event.setRating(getRating(event.getId()));
        return event;
    }

    public List<EventDto> enrichDtos(List<EventDto> events) {
        if (events == null || events.isEmpty()) {
            return events;
        }

        Map<Long, Long> counts = getConfirmedRequests(events.stream()
                .map(EventDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        events.forEach(event -> event.setConfirmedRequests(
                counts.getOrDefault(event.getId(), ValidationConstants.DEFAULT_CONFIRMED_REQUESTS)));
        Map<Long, Double> ratings = getRatings(events.stream()
                .map(EventDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        events.forEach(event -> event.setRating(ratings.getOrDefault(event.getId(), 0.0)));
        return events;
    }

    public List<EventShortDto> enrichShortDtos(List<EventShortDto> events) {
        if (events == null || events.isEmpty()) {
            return events;
        }

        Map<Long, Long> counts = getConfirmedRequests(events.stream()
                .map(EventShortDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        events.forEach(event -> event.setConfirmedRequests(
                counts.getOrDefault(event.getId(), ValidationConstants.DEFAULT_CONFIRMED_REQUESTS)));
        Map<Long, Double> ratings = getRatings(events.stream()
                .map(EventShortDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        events.forEach(event -> event.setRating(ratings.getOrDefault(event.getId(), 0.0)));
        return events;
    }

    private long getConfirmedRequests(Long eventId) {
        try {
            return requestServiceClient.getConfirmedRequestsCount(eventId);
        } catch (Exception e) {
            log.warn("Cannot get confirmed requests for event {}: {}", eventId, e.getMessage());
            return ValidationConstants.DEFAULT_CONFIRMED_REQUESTS;
        }
    }

    private Map<Long, Long> getConfirmedRequests(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            return requestServiceClient.getConfirmedRequestsCounts(eventIds);
        } catch (Exception e) {
            log.warn("Cannot get confirmed requests for events {}: {}", eventIds, e.getMessage());
            return Map.of();
        }
    }

    private double getRating(Long eventId) {
        try {
            RecommendationsClient recommendationsClient = recommendationsClientProvider.getIfAvailable();
            if (recommendationsClient == null) {
                return 0.0;
            }
            return recommendationsClient.getInteractionsCount(List.of(eventId)).stream()
                    .filter(score -> score.eventId() == eventId)
                    .findFirst()
                    .map(score -> score.score())
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Cannot get rating for event {}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    private Map<Long, Double> getRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            RecommendationsClient recommendationsClient = recommendationsClientProvider.getIfAvailable();
            if (recommendationsClient == null) {
                return Map.of();
            }
            return recommendationsClient.getInteractionsCount(eventIds).stream()
                    .collect(Collectors.toMap(
                            score -> score.eventId(),
                            score -> score.score(),
                            (first, second) -> first
                    ));
        } catch (Exception e) {
            log.warn("Cannot get ratings for events {}: {}", eventIds, e.getMessage());
            return Map.of();
        }
    }
}
