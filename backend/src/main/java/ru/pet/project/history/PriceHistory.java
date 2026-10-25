package ru.pet.project.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "price_history")
@IdClass(PriceHistoryId.class)
public class PriceHistory {

    @Id
    @Column(name = "link_id", nullable = false, updatable = false)
    private Long linkId;

    @Id
    @Column(name = "captured_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant capturedAt;

    @Column(name = "price", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected PriceHistory() {
    }

    public PriceHistory(Long linkId, Instant capturedAt, BigDecimal price) {
        this.linkId = linkId;
        this.capturedAt = capturedAt;
        this.price = price;
    }

    public Long getLinkId() {
        return linkId;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public BigDecimal getPrice() {
        return price;
    }
}