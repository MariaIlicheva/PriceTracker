package ru.pet.project.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Настройки писем из блока pricetracker.notifications в application.yaml
@ConfigurationProperties(prefix = "pricetracker.notifications")
public record NotificationProperties(String from,
                                     String siteUrl,
                                     long pollIntervalMs,
                                     int batchSize,
                                     Duration maxAge) {
}