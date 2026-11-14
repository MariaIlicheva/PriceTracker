package ru.pet.project.parsing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface ParseJobRepository extends JpaRepository<ParseJob, Long> {
    // Создаёт задачи на один сбор: по одной на каждую ссылку, за которой бы кто то следит
    // Уникальность (link_id, scheduled_at) не даёт создать дубль
    @Modifying
    @Query(value = """
            INSERT INTO parse_jobs (link_id, scheduled_at, status, attempts)
            SELECT DISTINCT pl.link_id, CAST(:slot AS timestamptz), 'pending', 0
            FROM product_links pl
                     JOIN products p ON p.id = pl.product_id
            WHERE p.status = 'active'
            ON CONFLICT (link_id, scheduled_at) DO NOTHING
            """, nativeQuery = true)
    int enqueueActiveLinks(@Param("slot") Instant slot);


    // Берёт следующую задачу и блокирует её строку до конца транзакции
    // SKIP LOCKED - строки, которые уже заблокировал другой обработчик, пропускаются
    // чтобы параллельные обработчики не получали одну и ту же задачу и не ждали друг друга
    // Неудачные задачи повторяются не раньше retryBefore
    @Query(value = """
            SELECT *
            FROM parse_jobs
            WHERE status = 'pending'
              AND scheduled_at <= now()
              AND (locked_at IS NULL OR locked_at < :retryBefore)
            ORDER BY scheduled_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<ParseJob> lockNextPending(@Param("retryBefore") Instant retryBefore);


    // Возвращает в очередь задачи, которые слишком долго в работе
    @Modifying
    @Query(value = """
            UPDATE parse_jobs
            SET status   = CASE WHEN attempts + 1 >= :maxAttempts THEN 'failed' ELSE 'pending' END,
                attempts = attempts + 1,
                error    = 'Обработчик не завершил задачу вовремя'
            WHERE status = 'processing'
              AND locked_at < :before
            """, nativeQuery = true)
    int recoverStale(@Param("before") Instant before, @Param("maxAttempts") short maxAttempts);
}