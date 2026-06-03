package ru.practicum.comment;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.internal.client.EventServiceClient;
import ru.practicum.internal.client.UserServiceClient;
import ru.practicum.web.comment.dto.CommentDto;
import ru.practicum.web.comment.dto.NewCommentDto;
import ru.practicum.web.comment.dto.UpdateCommentDto;
import ru.practicum.web.comment.entity.Comment;
import ru.practicum.web.comment.entity.CommentModerationStatus;
import ru.practicum.web.comment.mapper.CommentMapper;
import ru.practicum.web.comment.repository.CommentRepository;
import ru.practicum.web.comment.service.CommentService;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.user.entity.User;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@Transactional
public class FeignBackedCommentService implements CommentService {

    private final CommentRepository commentRepository;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getApprovedCommentsForEvent(Long eventId, int from, int size) {
        log.info("Get approved comments for event id={}", eventId);
        ensureEventPublished(eventId);
        validatePage(from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        return commentRepository.findByEventIdAndModerationStatus(eventId, CommentModerationStatus.APPROVED, pageable)
                .stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getApprovedComment(Long eventId, Long commentId) {
        log.info("Get approved comment id={} for event id={}", commentId, eventId);
        ensureEventPublished(eventId);
        Comment comment = commentRepository.findByIdAndEventId(commentId, eventId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        if (comment.getModerationStatus() != CommentModerationStatus.APPROVED) {
            throw new NotFoundException("Comment with id=" + commentId + " was not found");
        }
        return CommentMapper.toDto(comment);
    }

    @Override
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("Create comment for event id={} by user id={}", eventId, userId);
        ensureUserExists(userId);
        ensureEventPublished(eventId);

        Comment comment = Comment.builder()
                .author(entityManager.getReference(User.class, userId))
                .event(entityManager.getReference(Event.class, eventId))
                .text(dto.getText())
                .createdOn(LocalDateTime.now().withNano(0))
                .moderationStatus(CommentModerationStatus.PENDING)
                .build();

        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto updateOwnComment(Long userId, Long eventId, Long commentId, UpdateCommentDto dto) {
        log.info("Update comment id={} by user id={} for event id={}", commentId, userId, eventId);
        ensureUserExists(userId);
        ensureEventExists(eventId);
        Comment comment = commentRepository.findByIdAndAuthorIdAndEventId(commentId, userId, eventId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        comment.setText(dto.getText());
        comment.setModerationStatus(CommentModerationStatus.PENDING);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public void deleteOwnComment(Long userId, Long eventId, Long commentId) {
        log.info("Delete comment id={} by user id={} for event id={}", commentId, userId, eventId);
        ensureUserExists(userId);
        ensureEventExists(eventId);
        Comment comment = commentRepository.findByIdAndAuthorIdAndEventId(commentId, userId, eventId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        commentRepository.deleteById(comment.getId());
    }

    private void ensureUserExists(Long userId) {
        if (!userServiceClient.exists(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private void ensureEventExists(Long eventId) {
        if (!eventServiceClient.exists(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
    }

    private void ensureEventPublished(Long eventId) {
        EventDto event = eventServiceClient.getEvent(eventId);
        if (!"PUBLISHED".equals(event.getState())) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
    }

    private void validatePage(int from, int size) {
        if (from < ValidationConstants.PAGE_MIN_FROM) {
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }
        if (size <= 0) {
            throw new BadRequestException("Parameter 'size' must be positive");
        }
    }
}
