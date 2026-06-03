package ru.practicum.statsclient;

public class StatsServerUnavailable extends RuntimeException {

    public StatsServerUnavailable(String message, Throwable cause) {
        super(message, cause);
    }
}
