package ru.pet.project.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationSender {
    private final NotificationOutbox outbox;
    private final NotificationProperties properties;

    public NotificationSender(NotificationOutbox outbox, NotificationProperties properties) {
        this.outbox = outbox;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${pricetracker.notifications.poll-interval-ms}")
    public void sendPending() {
        for (int i = 0; i < properties.batchSize(); i++) {
            NotificationOutbox.Result result = outbox.sendNext();
            if (result == NotificationOutbox.Result.EMPTY || result == NotificationOutbox.Result.MAIL_UNAVAILABLE) {
                return;
            }
        }
    }
}