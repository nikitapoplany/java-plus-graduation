package ru.practicum.event;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.admin.entity.UpdateEventAdminRequest;
import ru.practicum.web.admin.mapper.AdminEventMapperService;
import ru.practicum.web.admin.repository.CategoryRepository;
import ru.practicum.web.admin.service.AdminEventServiceImpl;
import ru.practicum.web.admin.utils.DateUtils;
import ru.practicum.web.admin.validation.AdminEventValidator;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.stats.StatsService;

import java.util.List;

@Primary
@Service
@Transactional
public class FeignBackedAdminEventService extends AdminEventServiceImpl {

    private final EventResponseEnricher enricher;

    public FeignBackedAdminEventService(EventRepository eventRepository,
                                        CategoryRepository categoryRepository,
                                        StatsService statsService,
                                        AdminEventValidator validator,
                                        AdminEventMapperService mapperService,
                                        DateUtils dateUtils,
                                        EventResponseEnricher enricher) {
        super(eventRepository, categoryRepository, statsService, validator, mapperService, dateUtils);
        this.enricher = enricher;
    }

    @Override
    public List<EventDto> getEvents(List<Long> users,
                                    List<String> states,
                                    List<Long> categories,
                                    String rangeStart,
                                    String rangeEnd,
                                    int from,
                                    int size) {
        return enricher.enrichDtoRatings(super.getEvents(users, states, categories, rangeStart, rangeEnd, from, size));
    }

    @Override
    public EventDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest) {
        return enricher.enrichRating(super.updateEvent(eventId, updateRequest));
    }
}
