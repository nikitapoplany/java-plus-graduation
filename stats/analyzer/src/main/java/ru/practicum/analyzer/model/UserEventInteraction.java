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
@Table(name = "user_event_interactions")
public class UserEventInteraction {

    @EmbeddedId
    private UserEventInteractionId id;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "last_interaction_at", nullable = false)
    private Instant lastInteractionAt;

    public UserEventInteraction(Long userId, Long eventId, Double weight, Instant lastInteractionAt) {
        this.id = new UserEventInteractionId(userId, eventId);
        this.weight = weight;
        this.lastInteractionAt = lastInteractionAt;
    }
}
