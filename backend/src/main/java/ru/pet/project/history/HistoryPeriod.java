package ru.pet.project.history;

import java.time.Duration;

public enum HistoryPeriod {
    WEEK("week", Duration.ofDays(7)),
    MONTH("month", Duration.ofDays(30));

    private final String value;
    private final Duration length;

    HistoryPeriod(String value, Duration length) {
        this.value = value;
        this.length = length;
    }

    public String value() {
        return value;
    }

    public Duration length() {
        return length;
    }

    public static HistoryPeriod fromValue(String value) {
        for (HistoryPeriod period : values()) {
            if (period.value.equals(value)) {
                return period;
            }
        }
        throw new InvalidPeriodException();
    }
}