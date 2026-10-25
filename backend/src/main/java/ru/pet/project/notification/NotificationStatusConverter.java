package ru.pet.project.notification;

import jakarta.persistence.Converter;
import ru.pet.project.common.DbEnumConverter;

@Converter
public class NotificationStatusConverter extends DbEnumConverter<NotificationStatus> {
    public NotificationStatusConverter() {
        super(NotificationStatus.class);
    }
}