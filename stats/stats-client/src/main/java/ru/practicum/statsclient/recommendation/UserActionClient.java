package ru.practicum.statsclient.recommendation;

import java.time.Instant;

public interface UserActionClient {

    void collect(long userId, long eventId, UserActionType actionType, Instant timestamp);
}
