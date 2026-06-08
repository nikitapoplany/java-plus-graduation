package ru.practicum.aggregator.model;

public record EventPair(long first, long second) {

    public EventPair {
        long min = Math.min(first, second);
        long max = Math.max(first, second);
        first = min;
        second = max;
    }
}
