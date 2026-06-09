package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.EventSimilarityId;

import java.util.Collection;
import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    @Query("select similarity from EventSimilarity similarity "
            + "where similarity.id.eventA = :eventId or similarity.id.eventB = :eventId")
    List<EventSimilarity> findByEventId(@Param("eventId") Long eventId);

    @Query("select similarity from EventSimilarity similarity "
            + "where similarity.id.eventA in :eventIds or similarity.id.eventB in :eventIds")
    List<EventSimilarity> findByAnyEventIdIn(@Param("eventIds") Collection<Long> eventIds);
}
