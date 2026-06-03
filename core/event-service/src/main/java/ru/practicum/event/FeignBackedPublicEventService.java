package ru.practicum.event;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.event.service.PublicEventServiceImpl;

import java.util.List;

@Primary
@Service
@Transactional
public class FeignBackedPublicEventService extends PublicEventServiceImpl {

    private final EventResponseEnricher enricher;

    public FeignBackedPublicEventService(EventRepository eventRepository,
                                         StatsClient statsClient,
                                         EventResponseEnricher enricher) {
        super(eventRepository, statsClient);
        this.enricher = enricher;
    }

    @Override
    public EventDto getEvent(Long id) {
        return enricher.enrich(super.getEvent(id));
    }

    @Override
    public List<EventShortDto> getEvents(String text,
                                         List<Long> categories,
                                         Boolean paid,
                                         String rangeStart,
                                         String rangeEnd,
                                         Boolean onlyAvailable,
                                         String sort,
                                         int from,
                                         int size) {
        return enricher.enrichShortDtos(super.getEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size));
    }
}
