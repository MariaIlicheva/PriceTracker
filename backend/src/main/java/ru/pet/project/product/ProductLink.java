package ru.pet.project.product;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;


@Entity
@Table(name = "product_links")
public class ProductLink {

    @EmbeddedId
    private ProductLinkId id = new ProductLinkId();

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @MapsId("linkId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id")
    private Link link;

    @Column(name = "added_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant addedAt;

    protected ProductLink() {
    }

    ProductLink(Product product, Link link) {
        this.product = product;
        this.link = link;
        this.addedAt = Instant.now();
    }

    public ProductLinkId getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Link getLink() {
        return link;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductLink other)) {
            return false;
        }
        return Objects.equals(product, other.product) && Objects.equals(link, other.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, link);
    }
}