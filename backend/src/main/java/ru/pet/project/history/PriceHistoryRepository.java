package ru.pet.project.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, PriceHistoryId> {
    List<PriceHistory> findByLinkIdInAndCapturedAtBetweenOrderByCapturedAt(Collection<Long> linkIds, Instant from, Instant to);

    // Новый замер цены
    // Триггер price_history_after_insert обновит ссылку и создаст уведомления
    @Modifying
    @Query(value = "INSERT INTO price_history (link_id, captured_at, price) VALUES (:linkId, :capturedAt, :price)", nativeQuery = true)
    // почему INSERT, а не save()
    // у priceHistory ключ задаётся вручную (link_id и время)
    // на такой сущности save() сначала делает лишний SELECT, чтобы понять, новая она или нет.
    // прямой INSERT проще и честно показывает, что дальше работает триггер
    void insert(@Param("linkId") long linkId, @Param("capturedAt") Instant capturedAt, @Param("price") BigDecimal price);
}