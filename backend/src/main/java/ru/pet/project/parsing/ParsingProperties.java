package ru.pet.project.parsing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.Set;

// Настройки сбора цен из блока pricetracker.parsing в application.yaml
@ConfigurationProperties(prefix = "pricetracker.parsing")
public record ParsingProperties(String cron,
                                int workers,
                                long pollIntervalMs,
                                long requestDelayMs,
                                Duration retryDelay,
                                Duration staleAfter
                                ) {

}