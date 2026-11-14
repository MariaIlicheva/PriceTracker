package ru.pet.project.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

// Расписание сбора цен: создаёт задачи в 9:00 и 21:00 по Москве и возвращает в очередь зависшие
@Component
public class ParseScheduler {

    private static final Logger log = LoggerFactory.getLogger(ParseScheduler.class);

    private final ParseQueue queue;
    private final ParseWorkerPool workerPool;

    public ParseScheduler(ParseQueue queue, ParseWorkerPool workerPool) {
        this.queue = queue;
        this.workerPool = workerPool;
    }

    @Scheduled(cron = "${pricetracker.parsing.cron}", zone = "Europe/Moscow")
    public void enqueueJobs() {
        // Время сбора с точностью до минуты: повторный вызов в ту же минуту не создаст дублей
        Instant slot = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        int created = queue.enqueue(slot);
        log.info("Сбор цен {}: создано задач — {}", slot, created);
        workerPool.wakeUp();
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void recoverStaleJobs() {
        int recovered = queue.recoverStale();
        if (recovered > 0) {
            log.warn("Возвращено в очередь зависших задач: {}", recovered);
        }
    }
}