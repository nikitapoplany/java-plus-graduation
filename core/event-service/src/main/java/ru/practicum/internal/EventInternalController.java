package ru.practicum.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.NotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {

    private final EventRepository eventRepository;

    @GetMapping("/{eventId}")
    public EventDto getEvent(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(EventMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    @GetMapping("/{eventId}/exists")
    public boolean exists(@PathVariable Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @GetMapping("/{eventId}/initiator")
    public Long getInitiatorId(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getInitiator().getId())
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    @GetMapping("/{eventId}/confirmed-requests")
    public long getConfirmedRequests(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getConfirmedRequests() == null ? 0L : event.getConfirmedRequests())
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }
}
