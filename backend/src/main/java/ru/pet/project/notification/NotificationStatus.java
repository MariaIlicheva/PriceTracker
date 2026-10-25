package ru.pet.project.notification;

import ru.pet.project.common.DbEnum;

public enum NotificationStatus implements DbEnum {
    PENDING("pending"),
    SENT("sent"),
    FAILED("failed");

    private final String dbValue;

    NotificationStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}