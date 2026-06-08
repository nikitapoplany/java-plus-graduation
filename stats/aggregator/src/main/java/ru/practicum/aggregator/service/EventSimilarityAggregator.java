package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.config.AggregatorKafkaProperties;
import ru.practicum.aggregator.model.EventPair;
import ru.practicum.ewm.stats.avro.AvroBytes;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityAggregator {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final AggregatorKafkaProperties properties;

    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<EventPair, Double> minWeightSums = new HashMap<>();

    public synchronized void aggregate(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double newWeight = ActionWeight.of(action.getActionType());
        double oldWeight = eventUserWeights
                .getOrDefault(eventId, Map.of())
                .getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            log.debug("Skip action because max weight did not change: userId={}, eventId={}, old={}, new={}",
                    userId, eventId, oldWeight, newWeight);
            return;
        }

        List<EventPair> updatedPairs = updateMinWeights(eventId, userId, oldWeight, newWeight);
        eventUserWeights.computeIfAbsent(eventId, ignored -> new HashMap<>()).put(userId, newWeight);
        eventWeightSums.merge(eventId, newWeight - oldWeight, Double::sum);
        publishSimilarities(updatedPairs, action);
    }

    private List<EventPair> updateMinWeights(long changedEventId, long userId, double oldWeight, double newWeight) {
        List<EventPair> updatedPairs = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == changedEventId) {
                continue;
            }
            Double otherWeight = entry.getValue().get(userId);
            if (otherWeight == null) {
                continue;
            }

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double delta = newMin - oldMin;
            if (delta > 0) {
                EventPair pair = new EventPair(changedEventId, otherEventId);
                minWeightSums.merge(pair, delta, Double::sum);
                updatedPairs.add(pair);
            }
        }
        return updatedPairs;
    }

    private void publishSimilarities(List<EventPair> updatedPairs, UserActionAvro action) {
        if (updatedPairs.isEmpty()) {
            return;
        }

        long changedEventId = action.getEventId();
        double changedSum = eventWeightSums.getOrDefault(changedEventId, 0.0);
        if (changedSum == 0.0) {
            return;
        }

        for (EventPair pair : updatedPairs) {
            long otherEventId = pair.first() == changedEventId ? pair.second() : pair.first();
            if (otherEventId == changedEventId) {
                continue;
            }
            double score = calculateSimilarity(changedEventId, otherEventId, changedSum);
            if (score <= 0.0) {
                continue;
            }
            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(pair.first())
                    .setEventB(pair.second())
                    .setScore(score)
                    .setTimestamp(action.getTimestamp())
                    .build();
            kafkaTemplate.send(
                    properties.getTopics().getEventSimilarity(),
                    AvroBytes.serialize(similarity)
            );
            log.debug("Published event similarity: eventA={}, eventB={}, score={}",
                    pair.first(), pair.second(), score);
        }
    }

    private double calculateSimilarity(long changedEventId, long otherEventId, double changedSum) {
        double otherSum = eventWeightSums.getOrDefault(otherEventId, 0.0);
        if (otherSum == 0.0) {
            return 0.0;
        }
        double minSum = minWeightSums.getOrDefault(new EventPair(changedEventId, otherEventId), 0.0);
        return minSum / (Math.sqrt(changedSum) * Math.sqrt(otherSum));
    }
}
