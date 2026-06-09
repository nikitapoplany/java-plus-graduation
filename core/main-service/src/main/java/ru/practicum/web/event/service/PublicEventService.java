package ru.practicum.web.event.service;

import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;

import java.util.List;

public interface PublicEventService {

    EventDto getEvent(Long id);

    default EventDto getEvent(Long id, Long userId) {
        return getEvent(id);
    }

    List<EventShortDto> getEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    );

    default List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        return List.of();
    }

    default void likeEvent(Long userId, Long eventId) {
    }

    default List<EventDto> getEvents() {
        return List.of();
    }
}
