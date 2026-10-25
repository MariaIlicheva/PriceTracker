package ru.pet.project.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.pet.project.product.Link;
import ru.pet.project.product.Product;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    // ссылка, на которой цена опустилась ниже порога
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false, updatable = false)
    private Link link;

    @Column(name = "price", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Convert(converter = NotificationStatusConverter.class)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "sent_at", columnDefinition = "timestamptz")
    private Instant sentAt;

    protected Notification() {
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
        this.sentAt = null;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Link getLink() {
        return link;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}