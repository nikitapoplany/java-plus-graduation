package ru.practicum.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserEventInteraction;
import ru.practicum.analyzer.model.UserEventInteractionId;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserEventInteractionRepository
        extends JpaRepository<UserEventInteraction, UserEventInteractionId> {

    List<UserEventInteraction> findByIdUserIdOrderByLastInteractionAtDesc(Long userId, Pageable pageable);

    @Query("select interaction.id.eventId from UserEventInteraction interaction "
            + "where interaction.id.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("select interaction from UserEventInteraction interaction "
            + "where interaction.id.userId = :userId and interaction.id.eventId in :eventIds")
    List<UserEventInteraction> findByUserIdAndEventIds(@Param("userId") Long userId,
                                                       @Param("eventIds") Collection<Long> eventIds);

    @Query("select interaction.id.eventId as eventId, coalesce(sum(interaction.weight), 0) as score "
            + "from UserEventInteraction interaction "
            + "where interaction.id.eventId in :eventIds "
            + "group by interaction.id.eventId")
    List<EventInteractionScore> sumWeightsByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
