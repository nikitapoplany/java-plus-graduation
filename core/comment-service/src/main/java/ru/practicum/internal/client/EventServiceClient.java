package ru.practicum.internal.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.web.event.dto.EventDto;

@FeignClient(name = "EVENT-SERVICE", path = "/internal/events")
public interface EventServiceClient {

    @GetMapping("/{eventId}")
    EventDto getEvent(@PathVariable Long eventId);

    @GetMapping("/{eventId}/exists")
    boolean exists(@PathVariable Long eventId);
}
