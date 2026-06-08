package ru.practicum.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "event_similarities")
public class EventSimilarity {

    @EmbeddedId
    private EventSimilarityId id;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public EventSimilarity(Long eventA, Long eventB, Double score, Instant updatedAt) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        this.id = new EventSimilarityId(first, second);
        this.score = score;
        this.updatedAt = updatedAt;
    }

    public Long getOtherEventId(Long eventId) {
        if (id.getEventA().equals(eventId)) {
            return id.getEventB();
        }
        return id.getEventA();
    }
}
