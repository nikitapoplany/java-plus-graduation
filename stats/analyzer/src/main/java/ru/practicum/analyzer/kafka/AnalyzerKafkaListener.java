package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.avro.AvroBytes;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerKafkaListener {

    private final RecommendationService recommendationService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-actions:stats.user-actions.v1}",
            groupId = "${spring.kafka.consumer.group-id:analyzer}"
    )
    public void onUserAction(byte[] payload) {
        UserActionAvro action = AvroBytes.deserialize(payload, UserActionAvro.getClassSchema());
        recommendationService.save(action);
        log.debug("Saved user interaction: userId={}, eventId={}, type={}",
                action.getUserId(), action.getEventId(), action.getActionType());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.event-similarity:stats.events-similarity.v1}",
            groupId = "${spring.kafka.consumer.group-id:analyzer}"
    )
    public void onEventSimilarity(byte[] payload) {
        EventSimilarityAvro similarity = AvroBytes.deserialize(payload, EventSimilarityAvro.getClassSchema());
        recommendationService.save(similarity);
        log.debug("Saved event similarity: eventA={}, eventB={}, score={}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
    }
}
