package ru.practicum.internal.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.web.event.dto.EventDto;

@FeignClient(name = "EVENT-SERVICE", path = "/internal/events")
public interface EventServiceClient {

    @GetMapping("/{eventId}")
    EventDto getEvent(@PathVariable Long eventId);

    @GetMapping("/{eventId}/exists")
    boolean exists(@PathVariable Long eventId);

    @GetMapping("/{eventId}/initiator")
    Long getInitiatorId(@PathVariable Long eventId);

    @PostMapping("/{eventId}/confirmed-requests")
    long addConfirmedRequests(@PathVariable Long eventId, @RequestParam("delta") long delta);
}
