package ru.pet.project.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.pet.project.marketplace.Marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marketplace_id", nullable = false, updatable = false)
    private Marketplace marketplace;

    @Column(name = "url", nullable = false, updatable = false, columnDefinition = "text")
    private String url;

    @Column(name = "last_price", precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal lastPrice;

    @Column(name = "last_checked_at", insertable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant lastCheckedAt;

    protected Link() {
    }

    public Link(Marketplace marketplace, String url) {
        this.marketplace = marketplace;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getUrl() {
        return url;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Link other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}