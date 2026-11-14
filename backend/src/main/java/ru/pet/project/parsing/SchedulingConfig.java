package ru.pet.project.parsing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Включает выполнение методов с @Scheduled и чтение ParsingProperties
@Configuration
@EnableScheduling
@EnableConfigurationProperties(ParsingProperties.class)
public class SchedulingConfig {

}