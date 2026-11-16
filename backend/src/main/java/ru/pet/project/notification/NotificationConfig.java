package ru.pet.project.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Регистрирует настройки писем. Расписание уже включено в SchedulingConfig
@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {
}