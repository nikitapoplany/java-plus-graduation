package ru.practicum.event;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserViewRegistry {

    private final Set<ViewKey> views = ConcurrentHashMap.newKeySet();

    public void markViewed(long userId, long eventId) {
        views.add(new ViewKey(userId, eventId));
    }

    public boolean hasViewed(long userId, long eventId) {
        return views.contains(new ViewKey(userId, eventId));
    }

    private record ViewKey(long userId, long eventId) {
    }
}
