package ru.practicum.internal.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "REQUEST-SERVICE", path = "/internal/requests")
public interface RequestServiceClient {

    @GetMapping("/events/{eventId}/confirmed-count")
    long getConfirmedRequestsCount(@PathVariable Long eventId);
}
