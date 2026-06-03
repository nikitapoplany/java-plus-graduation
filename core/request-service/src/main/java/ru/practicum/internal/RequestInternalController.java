package ru.practicum.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.request.repository.ParticipationRequestRepository;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {

    private final ParticipationRequestRepository requestRepository;

    @GetMapping("/events/{eventId}/confirmed-count")
    public long getConfirmedRequestsCount(@PathVariable Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }
}
