package ru.pet.project.parsing;

import ru.pet.project.common.DbEnum;

public enum ParseJobStatus implements DbEnum {

    PENDING("pending"),
    PROCESSING("processing"),
    DONE("done"),
    FAILED("failed");

    private final String dbValue;

    ParseJobStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}