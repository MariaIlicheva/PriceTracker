package ru.pet.project.product;

import ru.pet.project.common.DbEnum;

public enum ProductStatus implements DbEnum {

    ACTIVE("active"),
    PAUSED("paused");

    private final String dbValue;

    ProductStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}