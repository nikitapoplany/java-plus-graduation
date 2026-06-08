package ru.practicum.request;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.internal.client.EventServiceClient;
import ru.practicum.internal.client.UserServiceClient;
import ru.practicum.statsclient.recommendation.UserActionClient;
import ru.practicum.statsclient.recommendation.UserActionType;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.web.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.web.request.dto.ParticipationRequestDto;
import ru.practicum.web.request.entity.ParticipationRequest;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.request.mapper.ParticipationRequestMapper;
import ru.practicum.web.request.repository.ParticipationRequestRepository;
import ru.practicum.web.request.service.PrivateRequestService;
import ru.practicum.web.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@Transactional
public class FeignBackedPrivateRequestService implements PrivateRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final ObjectProvider<UserActionClient> userActionClientProvider;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        checkUserExists(userId);
        EventDto event = getEvent(eventId);

        validateAddRequest(userId, eventId, event);

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now().withNano(0))
                .event(entityManager.getReference(Event.class, eventId))
                .requester(entityManager.getReference(User.class, userId))
                .status(determineInitialStatus(event))
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        if (saved.getStatus() == RequestStatus.CONFIRMED) {
            eventServiceClient.addConfirmedRequests(eventId, 1L);
        }
        collectRegisterAction(userId, eventId);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        checkUserExists(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request not found");
        }

        boolean wasConfirmed = request.getStatus() == RequestStatus.CONFIRMED;
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);

        if (wasConfirmed) {
            eventServiceClient.addConfirmedRequests(request.getEvent().getId(), -1L);
        }
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        EventDto event = getEvent(eventId);
        checkEventOwner(event, userId, eventId);

        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest statusUpdateRequest
    ) {
        checkUserExists(userId);
        EventDto event = getEvent(eventId);
        checkEventOwner(event, userId, eventId);
        validateEventForModeration(event, eventId);

        List<ParticipationRequest> requests = validateAndGetRequestsForUpdate(
                statusUpdateRequest.getRequestIds(), eventId);

        if ("CONFIRMED".equals(statusUpdateRequest.getStatus())) {
            return confirmRequests(eventId, event, requests);
        }
        if ("REJECTED".equals(statusUpdateRequest.getStatus())) {
            return rejectRequests(requests);
        }

        throw new ConflictException("Invalid status: " + statusUpdateRequest.getStatus());
    }

    private void checkUserExists(Long userId) {
        if (!userServiceClient.exists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private EventDto getEvent(Long eventId) {
        return eventServiceClient.getEvent(eventId);
    }

    private void validateAddRequest(Long userId, Long eventId, EventDto event) {
        if (event.getInitiator() != null && userId.equals(event.getInitiator().getId())) {
            throw new ConflictException("Initiator cannot participate in his own event");
        }
        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Event is not published");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        int participantLimit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        int confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (participantLimit > 0 && confirmedRequests >= participantLimit) {
            throw new ConflictException("Participant limit reached");
        }
    }

    private RequestStatus determineInitialStatus(EventDto event) {
        int participantLimit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        boolean requestModeration = event.getRequestModeration() == null || event.getRequestModeration();
        if (participantLimit == 0 || !requestModeration) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }

    private void checkEventOwner(EventDto event, Long userId, Long eventId) {
        if (event.getInitiator() == null || !userId.equals(event.getInitiator().getId())) {
            throw new NotFoundException("Event with id=" + eventId + " was not found for user " + userId);
        }
    }

    private void validateEventForModeration(EventDto event, Long eventId) {
        int participantLimit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        boolean requestModeration = event.getRequestModeration() == null || event.getRequestModeration();
        if (participantLimit == 0 || !requestModeration) {
            log.warn("Event {} does not require request moderation", eventId);
            throw new ConflictException("The participant limit has been reached");
        }
    }

    private List<ParticipationRequest> validateAndGetRequestsForUpdate(List<Long> requestIds, Long eventId) {
        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);
        if (requests.isEmpty()) {
            throw new ConflictException("Request ids list is empty");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Request with id=" + request.getId() + " not found for this event");
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        return requests;
    }

    private EventRequestStatusUpdateResult confirmRequests(
            Long eventId,
            EventDto event,
            List<ParticipationRequest> requests
    ) {
        int participantLimit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();
        int confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (confirmedRequests >= participantLimit) {
            throw new ConflictException("The participant limit has been reached");
        }

        int availableSlots = participantLimit - confirmedRequests;
        List<ParticipationRequestDto> confirmedList = new ArrayList<>();
        List<ParticipationRequestDto> rejectedList = new ArrayList<>();

        int confirmedCount = 0;
        for (ParticipationRequest request : requests) {
            if (confirmedCount < availableSlots) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmedList.add(ParticipationRequestMapper.toDto(request));
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedList.add(ParticipationRequestMapper.toDto(request));
            }
            requestRepository.save(request);
        }

        if (confirmedCount > 0) {
            eventServiceClient.addConfirmedRequests(eventId, confirmedCount);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedList)
                .rejectedRequests(rejectedList)
                .build();
    }

    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        List<ParticipationRequestDto> rejectedList = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            request.setStatus(RequestStatus.REJECTED);
            requestRepository.save(request);
            rejectedList.add(ParticipationRequestMapper.toDto(request));
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(new ArrayList<>())
                .rejectedRequests(rejectedList)
                .build();
    }

    private void collectRegisterAction(Long userId, Long eventId) {
        try {
            UserActionClient userActionClient = userActionClientProvider.getIfAvailable();
            if (userActionClient == null) {
                return;
            }
            userActionClient.collect(userId, eventId, UserActionType.REGISTER, java.time.Instant.now());
        } catch (Exception e) {
            log.warn("Cannot send REGISTER action for user {} and event {}: {}", userId, eventId, e.getMessage());
        }
    }
}
