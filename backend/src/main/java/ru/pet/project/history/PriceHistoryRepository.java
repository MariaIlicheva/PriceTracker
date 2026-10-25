package ru.pet.project.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, PriceHistoryId> {
    List<PriceHistory> findByLinkIdInAndCapturedAtBetweenOrderByCapturedAt(Collection<Long> linkIds, Instant from, Instant to);
}