package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.AvroBytes;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionKafkaListener {

    private final EventSimilarityAggregator aggregator;

    @KafkaListener(
            topics = "${app.kafka.topics.user-actions:stats.user-actions.v1}",
            groupId = "${spring.kafka.consumer.group-id:aggregator}"
    )
    public void onMessage(byte[] payload) {
        UserActionAvro action = AvroBytes.deserialize(payload, UserActionAvro.getClassSchema());
        log.debug("Received user action: userId={}, eventId={}, type={}",
                action.getUserId(), action.getEventId(), action.getActionType());
        aggregator.aggregate(action);
    }
}
