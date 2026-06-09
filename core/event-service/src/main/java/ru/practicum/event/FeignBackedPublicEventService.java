package ru.practicum.event;

import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsclient.recommendation.RecommendationsClient;
import ru.practicum.statsclient.recommendation.UserActionClient;
import ru.practicum.statsclient.recommendation.UserActionType;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.event.service.PublicEventServiceImpl;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.NotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Service
@Transactional
public class FeignBackedPublicEventService extends PublicEventServiceImpl {

    private final EventResponseEnricher enricher;
    private final EventRepository eventRepository;
    private final ObjectProvider<UserActionClient> userActionClientProvider;
    private final ObjectProvider<RecommendationsClient> recommendationsClientProvider;
    private final UserViewRegistry userViewRegistry;

    public FeignBackedPublicEventService(EventRepository eventRepository,
                                         StatsClient statsClient,
                                         EventResponseEnricher enricher,
                                         ObjectProvider<UserActionClient> userActionClientProvider,
                                         ObjectProvider<RecommendationsClient> recommendationsClientProvider,
                                         UserViewRegistry userViewRegistry) {
        super(eventRepository, statsClient);
        this.eventRepository = eventRepository;
        this.enricher = enricher;
        this.userActionClientProvider = userActionClientProvider;
        this.recommendationsClientProvider = recommendationsClientProvider;
        this.userViewRegistry = userViewRegistry;
    }

    @Override
    public EventDto getEvent(Long id) {
        return enricher.enrich(super.getEvent(id));
    }

    @Override
    public EventDto getEvent(Long id, Long userId) {
        EventDto event = getEvent(id);
        if (userId != null) {
            if (userId <= 0) {
                throw new BadRequestException("Header X-EWM-USER-ID must be positive");
            }
            collectAction(userId, id, UserActionType.VIEW);
            userViewRegistry.markViewed(userId, id);
        }
        return event;
    }

    @Override
    public List<EventShortDto> getEvents(String text,
                                         List<Long> categories,
                                         Boolean paid,
                                         String rangeStart,
                                         String rangeEnd,
                                         Boolean onlyAvailable,
                                         String sort,
                                         int from,
                                         int size) {
        return enricher.enrichShortDtos(super.getEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size));
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Header X-EWM-USER-ID must be positive");
        }
        try {
            RecommendationsClient recommendationsClient = recommendationsClientProvider.getIfAvailable();
            if (recommendationsClient == null) {
                return List.of();
            }
            Map<Long, Double> scores = recommendationsClient.getRecommendationsForUser(userId, maxResults).stream()
                    .collect(Collectors.toMap(
                            score -> score.eventId(),
                            score -> score.score(),
                            Math::max,
                            HashMap::new
                    ));
            if (scores.isEmpty()) {
                return List.of();
            }

            Map<Long, Event> events = eventRepository.findAllById(scores.keySet()).stream()
                    .filter(event -> event.getStatus() == EventStatus.PUBLISHED)
                    .collect(Collectors.toMap(Event::getId, event -> event));

            List<EventShortDto> result = new ArrayList<>();
            scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .forEach(entry -> {
                        Event event = events.get(entry.getKey());
                        if (event != null) {
                            EventShortDto dto = EventMapper.toShortDto(event);
                            dto.setRating(entry.getValue());
                            result.add(dto);
                        }
                    });
            List<EventShortDto> enriched = enricher.enrichShortDtos(result);
            enriched.forEach(event -> event.setRating(scores.getOrDefault(event.getId(), event.getRating())));
            return enriched.stream()
                    .sorted(Comparator.comparing(EventShortDto::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void likeEvent(Long userId, Long eventId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("Header X-EWM-USER-ID must be positive");
        }
        boolean publishedExists = eventRepository.existsById(eventId)
                && eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED).isPresent();
        if (!publishedExists) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        if (!userViewRegistry.hasViewed(userId, eventId)) {
            throw new BadRequestException("User can like only events he has viewed");
        }
        collectAction(userId, eventId, UserActionType.LIKE);
    }

    private void collectAction(Long userId, Long eventId, UserActionType actionType) {
        try {
            UserActionClient userActionClient = userActionClientProvider.getIfAvailable();
            if (userActionClient == null) {
                return;
            }
            userActionClient.collect(userId, eventId, actionType, Instant.now());
        } catch (Exception ignored) {
            // User-facing event endpoints should stay available if the recommendation pipeline is temporarily down.
        }
    }
}
