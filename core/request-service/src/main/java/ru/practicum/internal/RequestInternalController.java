package ru.practicum.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.request.repository.ParticipationRequestRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {

    private final ParticipationRequestRepository requestRepository;

    @GetMapping("/events/{eventId}/confirmed-count")
    public long getConfirmedRequestsCount(@PathVariable Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @GetMapping("/events/confirmed-counts")
    public Map<Long, Long> getConfirmedRequestsCounts(@RequestParam("eventIds") List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
