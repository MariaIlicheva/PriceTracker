package ru.pet.project.common;

import jakarta.persistence.AttributeConverter;

public abstract class DbEnumConverter<E extends Enum<E> & DbEnum> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected DbEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        for (E value : type.getEnumConstants()) {
            if (value.dbValue().equals(dbData)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Неизвестное значение " + dbData + " для " + type.getSimpleName());
    }
}