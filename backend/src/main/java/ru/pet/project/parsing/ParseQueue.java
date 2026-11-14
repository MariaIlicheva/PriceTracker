package ru.pet.project.parsing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pet.project.history.PriceHistoryRepository;
import ru.pet.project.product.Link;
import ru.pet.project.product.LinkRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

// это диспетчер очереди - работает с таблицей parse_jobs
// все методы отдельные короткин транзакции
// пока парсер ходит на сайт, ни одна транзакция не открыта и строки не заблокированы.
@Service
public class ParseQueue {

    private static final int MAX_ERROR_LENGTH = 1000;
    private final ParseJobRepository parseJobRepository; //запросы к очереди
    private final PriceHistoryRepository priceHistoryRepository; // запись замера цены
    private final ParsingProperties properties;

    public ParseQueue(ParseJobRepository parseJobRepository,
                      PriceHistoryRepository priceHistoryRepository,
                      ParsingProperties properties) {
        this.parseJobRepository = parseJobRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.properties = properties;
    }

    @Transactional
    public int enqueue(Instant slot) { //создание задач
        return parseJobRepository.enqueueActiveLinks(slot);
    }

    // Транзакция 1: взять задачу и отметить ее в работе
    @Transactional
    public Optional<ClaimedJob> claimNext() {
        Instant retryBefore = Instant.now().minus(properties.retryDelay());
        return parseJobRepository.lockNextPending(retryBefore).map(job -> {
            job.start();
            Link link = job.getLink();
            ParseTarget target = new ParseTarget(link.getUrl(), link.getMarketplace().getCode());
            return new ClaimedJob(job.getId(), link.getId(), job.getAttempts() + 1, target);
        });
        //блокировка строки снимается
    }


     // Транзакция 2: записать цену и закрыть задачу
    @Transactional
    public void complete(long jobId, long linkId, BigDecimal price) {
        // Пока шёл парсинг, пользователь мог удалить товар — тогда ссылка и задача удалены каскадом
        parseJobRepository.findById(jobId).ifPresent(job -> {
            priceHistoryRepository.insert(linkId, Instant.now(), price);
            //вызывается когда парсер нашел ценц
            job.complete();
        });
    }

    @Transactional
    public void fail(long jobId, String error) {
        parseJobRepository.findById(jobId).ifPresent(job -> job.fail(shorten(error)));
    }

    @Transactional
    public int recoverStale() { //вернуть застывшие задачи
        Instant before = Instant.now().minus(properties.staleAfter());
        return parseJobRepository.recoverStale(before, ParseJob.MAX_ATTEMPTS);
    }

    private static String shorten(String text) { // обрезать текст ошибки
        if (text == null) {
            return "Неизвестная ошибка";
        }
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }
}