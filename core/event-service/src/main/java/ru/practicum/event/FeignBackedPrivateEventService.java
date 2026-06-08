package ru.practicum.event;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.internal.client.UserServiceClient;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.web.admin.entity.Category;
import ru.practicum.web.admin.repository.CategoryRepository;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.dto.NewEventDto;
import ru.practicum.web.event.dto.UpdateEventUserRequest;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.event.service.PrivateEventService;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.user.entity.User;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@Transactional
public class FeignBackedPrivateEventService implements PrivateEventService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final UserServiceClient userServiceClient;
    private final EventResponseEnricher enricher;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        log.info("Get events for user id={}, from={}, size={}", userId, from, size);
        ensureUserExists(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        Map<Long, Long> viewsMap = getViewsMap(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), ValidationConstants.DEFAULT_VIEWS));
                    return dto;
                })
                .collect(Collectors.toList());

        return enricher.enrichShortDtos(result);
    }

    @Override
    public EventDto addEvent(Long userId, NewEventDto dto) {
        log.info("Create event by user id={}", userId);
        ensureUserExists(userId);

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));

        LocalDateTime eventDate = parseDateTime(dto.getEventDate());
        if (eventDate.isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_HOURS_BEFORE_START))) {
            throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: "
                    + dto.getEventDate());
        }

        Event event = Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(eventDate)
                .initiator(entityManager.getReference(User.class, userId))
                .category(category)
                .location(dto.getLocation())
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .status(EventStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(ValidationConstants.DEFAULT_CONFIRMED_REQUESTS)
                .views(ValidationConstants.DEFAULT_VIEWS)
                .build();

        return enricher.enrich(EventMapper.toDto(eventRepository.save(event)));
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getEvent(Long userId, Long eventId) {
        log.info("Get event id={} for user id={}", eventId, userId);
        ensureUserExists(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        EventDto dto = EventMapper.toDto(event);
        dto.setViews(getViewsForEvent(event));
        return enricher.enrich(dto);
    }

    @Override
    public EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Update event id={} by user id={}", eventId, userId);
        ensureUserExists(userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        applyUserUpdate(event, updateRequest);

        Event updated = eventRepository.save(event);
        EventDto dto = EventMapper.toDto(updated);
        dto.setViews(getViewsForEvent(updated));
        return enricher.enrich(dto);
    }

    private void ensureUserExists(Long userId) {
        if (!userServiceClient.exists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private void applyUserUpdate(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(updateRequest.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_HOURS_BEFORE_START))) {
                throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: "
                        + updateRequest.getEventDate());
            }
            event.setEventDate(newEventDate);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < ValidationConstants.EVENT_PARTICIPANT_LIMIT_MIN) {
                throw new BadRequestException("Participant limit must be non-negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id="
                            + updateRequest.getCategory() + " was not found"));
            event.setCategory(category);
        }
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setStatus(EventStatus.PENDING);
                case "CANCEL_REVIEW" -> event.setStatus(EventStatus.CANCELED);
                default -> throw new BadRequestException("Invalid state action: " + updateRequest.getStateAction());
            }
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (Exception e2) {
                throw new BadRequestException("Invalid date format. Expected: " + ValidationConstants.DATE_TIME_FORMAT);
            }
        }
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, LocalDateTime.now(), uris, true);
            return stats.stream()
                    .filter(stat -> stat.getUri() != null)
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.warn("Cannot get event views: {}", e.getMessage());
            return Map.of();
        }
    }

    private Long getViewsForEvent(Event event) {
        if (event.getId() == null) {
            return ValidationConstants.DEFAULT_VIEWS;
        }

        try {
            LocalDateTime start = event.getCreatedOn() != null
                    ? event.getCreatedOn()
                    : LocalDateTime.now().minusYears(1);
            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    LocalDateTime.now(),
                    List.of("/events/" + event.getId()),
                    true
            );
            return stats.isEmpty() ? ValidationConstants.DEFAULT_VIEWS : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Cannot get views for event {}: {}", event.getId(), e.getMessage());
            return ValidationConstants.DEFAULT_VIEWS;
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }
}
