package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.EventSimilarityId;
import ru.practicum.analyzer.model.UserEventInteraction;
import ru.practicum.analyzer.model.UserEventInteractionId;
import ru.practicum.analyzer.repository.EventInteractionScore;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int RECENT_INTERACTIONS_LIMIT = 20;
    private static final int NEAREST_NEIGHBORS_LIMIT = 10;

    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    @Transactional
    public void save(UserActionAvro action) {
        UserEventInteractionId id = new UserEventInteractionId(action.getUserId(), action.getEventId());
        double newWeight = ActionWeight.of(action.getActionType());
        UserEventInteraction interaction = interactionRepository.findById(id)
                .orElseGet(() -> new UserEventInteraction(
                        action.getUserId(),
                        action.getEventId(),
                        newWeight,
                        action.getTimestamp()
                ));

        if (newWeight >= interaction.getWeight()) {
            interaction.setWeight(newWeight);
            interaction.setLastInteractionAt(action.getTimestamp());
            interactionRepository.save(interaction);
        }
    }

    @Transactional
    public void save(EventSimilarityAvro similarityAvro) {
        long first = Math.min(similarityAvro.getEventA(), similarityAvro.getEventB());
        long second = Math.max(similarityAvro.getEventA(), similarityAvro.getEventB());
        EventSimilarityId id = new EventSimilarityId(first, second);
        EventSimilarity similarity = similarityRepository.findById(id)
                .orElseGet(() -> new EventSimilarity(first, second, similarityAvro.getScore(), similarityAvro.getTimestamp()));

        similarity.setScore(similarityAvro.getScore());
        similarity.setUpdatedAt(similarityAvro.getTimestamp());
        similarityRepository.save(similarity);
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        Set<Long> interactedEventIds = interactionRepository.findEventIdsByUserId(userId);

        return similarityRepository.findByEventId(eventId).stream()
                .map(similarity -> new RecommendedEvent(similarity.getOtherEventId(eventId), similarity.getScore()))
                .filter(event -> !interactedEventIds.contains(event.eventId()))
                .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                .limit(normalizeLimit(maxResults))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        int limit = normalizeLimit(maxResults);
        List<UserEventInteraction> recentInteractions = interactionRepository
                .findByIdUserIdOrderByLastInteractionAtDesc(
                        userId,
                        PageRequest.of(0, Math.max(limit, RECENT_INTERACTIONS_LIMIT))
                );
        if (recentInteractions.isEmpty()) {
            return List.of();
        }

        Set<Long> interactedEventIds = new HashSet<>();
        for (UserEventInteraction interaction : recentInteractions) {
            interactedEventIds.add(interaction.getId().getEventId());
        }

        List<EventSimilarity> candidateSimilarities = similarityRepository.findByAnyEventIdIn(interactedEventIds);
        Map<Long, Double> candidateScores = new HashMap<>();
        for (EventSimilarity similarity : candidateSimilarities) {
            Long candidateId = candidateId(similarity, interactedEventIds);
            if (candidateId != null && !interactedEventIds.contains(candidateId)) {
                candidateScores.merge(candidateId, similarity.getScore(), Math::max);
            }
        }

        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new RecommendedEvent(entry.getKey(), predictScore(entry.getKey(), userId, interactedEventIds)))
                .filter(event -> event.score() > 0.0)
                .sorted(Comparator.comparingDouble(RecommendedEvent::score).reversed())
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendedEvent> getInteractionsCount(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Double> scores = new HashMap<>();
        for (Long eventId : eventIds) {
            scores.put(eventId, 0.0);
        }
        for (EventInteractionScore score : interactionRepository.sumWeightsByEventIds(eventIds)) {
            scores.put(score.getEventId(), Optional.ofNullable(score.getScore()).orElse(0.0));
        }
        return scores.entrySet().stream()
                .map(entry -> new RecommendedEvent(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(RecommendedEvent::eventId))
                .toList();
    }

    private double predictScore(long candidateId, long userId, Set<Long> interactedEventIds) {
        Map<Long, Double> similarities = new HashMap<>();
        for (EventSimilarity similarity : similarityRepository.findByEventId(candidateId)) {
            Long neighborId = similarity.getOtherEventId(candidateId);
            if (interactedEventIds.contains(neighborId)) {
                similarities.put(neighborId, similarity.getScore());
            }
        }

        List<Long> nearestNeighbors = similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(NEAREST_NEIGHBORS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
        if (nearestNeighbors.isEmpty()) {
            return 0.0;
        }

        Map<Long, Double> userWeights = new HashMap<>();
        for (UserEventInteraction interaction : interactionRepository.findByUserIdAndEventIds(userId, nearestNeighbors)) {
            userWeights.put(interaction.getId().getEventId(), interaction.getWeight());
        }

        double weightedSum = 0.0;
        double similaritySum = 0.0;
        for (Long neighborId : nearestNeighbors) {
            double similarity = similarities.getOrDefault(neighborId, 0.0);
            double weight = userWeights.getOrDefault(neighborId, 0.0);
            weightedSum += similarity * weight;
            similaritySum += similarity;
        }
        return similaritySum == 0.0 ? 0.0 : weightedSum / similaritySum;
    }

    private Long candidateId(EventSimilarity similarity, Set<Long> interactedEventIds) {
        Long eventA = similarity.getId().getEventA();
        Long eventB = similarity.getId().getEventB();
        if (interactedEventIds.contains(eventA) && !interactedEventIds.contains(eventB)) {
            return eventB;
        }
        if (interactedEventIds.contains(eventB) && !interactedEventIds.contains(eventA)) {
            return eventA;
        }
        return null;
    }

    private int normalizeLimit(int maxResults) {
        return maxResults > 0 ? maxResults : 10;
    }
}
