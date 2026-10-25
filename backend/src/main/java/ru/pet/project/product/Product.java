package ru.pet.project.product;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import ru.pet.project.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "target_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Convert(converter = ProductStatusConverter.class)
    @Column(name = "status", nullable = false, length = 10)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductLink> links = new LinkedHashSet<>();

    protected Product() {
    }

    public Product(User user, String name, BigDecimal targetPrice) {
        this.user = user;
        this.name = name;
        this.targetPrice = targetPrice;
        this.status = ProductStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void addLink(Link link) {
        links.add(new ProductLink(this, link));
    }

    public void removeLink(Link link) {
        links.removeIf(productLink -> productLink.getLink().equals(link));
    }

    public void pause() {
        this.status = ProductStatus.PAUSED;
    }

    public void resume() {
        this.status = ProductStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<ProductLink> getLinks() {
        return Collections.unmodifiableSet(links);
    }
}