package ru.practicum.web.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.service.PublicEventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final PublicEventService publicEventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(required = false) Boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /events with params: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                        "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        List<EventShortDto> events = publicEventService.getEvents(
                text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size
        );
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEvent(@PathVariable Long id,
                                             @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        log.info("GET /events/{}", id);

        EventDto event = publicEventService.getEvent(id, userId);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<EventShortDto>> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") int maxResults
    ) {
        return ResponseEntity.ok(publicEventService.getRecommendations(userId, maxResults));
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> likeEvent(@PathVariable Long eventId,
                                          @RequestHeader("X-EWM-USER-ID") Long userId) {
        publicEventService.likeEvent(userId, eventId);
        return ResponseEntity.ok().build();
    }
}
