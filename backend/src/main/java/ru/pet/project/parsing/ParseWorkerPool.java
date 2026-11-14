package ru.pet.project.parsing;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// Несколько обработчиков, которые параллельно разбирают очередь parse_jobs
// Каждый в своём потоке берёт задачу за задачей, пока очередь не опустеет, затем засыпает
// до следующей проверки или до нового сбора цен.
@Component
public class ParseWorkerPool {
    //через логгер сообщения попадают в консоль идеи
    private static final Logger log = LoggerFactory.getLogger(ParseWorkerPool.class);
    private final ParseQueue queue; // диспетчер очереди
    private final PriceParser parser; // доставатель цены с сайта
    private final ParsingProperties properties; // настройки
    private final ExecutorService executor; // пул потоков которые ждут работу
    private final AtomicInteger busyWorkers = new AtomicInteger(); //счетчик занятых работников

    public ParseWorkerPool(ParseQueue queue, PriceParser parser, ParsingProperties properties) {
        this.queue = queue;
        this.parser = parser;
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(properties.workers(), new CustomizableThreadFactory("parse-worker-"));
    }

    @Scheduled(fixedDelayString = "${pricetracker.parsing.poll-interval-ms}")
    public synchronized void wakeUp() { //synchronized - метод выполняется только одним потоком
        while (busyWorkers.get() < properties.workers()) {
            busyWorkers.incrementAndGet();
            executor.submit(() -> drainQueue());
        }
    }

    private void drainQueue() { //жизнь одного работника
        try {
            while (!Thread.currentThread().isInterrupted() && processNext()) {
                Thread.sleep(properties.requestDelayMs());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            log.error("Обработчик очереди остановился с ошибкой", e);
        } finally {
            busyWorkers.decrementAndGet();
        }
    }

    private boolean processNext() { //работа с задачей целеком
        Optional<ClaimedJob> claimed = queue.claimNext();
        if (claimed.isEmpty()) {
            return false;
        }
        ClaimedJob job = claimed.get(); //взяли задачу
        try {
            //попытка сходить на сайт
            BigDecimal price = parser.fetchPrice(job.target());
            queue.complete(job.jobId(), job.linkId(), price); //запись результата
            log.info("Задача {}: {} — {} rub", job.jobId(), job.target().url(), price);
        } catch (ParseFailedException e) {
            queue.fail(job.jobId(), e.getMessage());
            log.warn("Задача {}, попытка {}: {} — {}", job.jobId(), job.attempt(), job.target().url(), e.getMessage());
        } catch (RuntimeException e) {
            queue.fail(job.jobId(), "Внутренняя ошибка: " + e.getMessage());
            log.error("Задача {}: внутренняя ошибка", job.jobId(), e);
        }
        return true;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}