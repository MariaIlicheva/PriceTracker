package ru.pet.project.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ProductLinkId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "link_id")
    private Long linkId;

    protected ProductLinkId() {
    }

    public ProductLinkId(Long productId, Long linkId) {
        this.productId = productId;
        this.linkId = linkId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getLinkId() {
        return linkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductLinkId other)) {
            return false;
        }
        return Objects.equals(productId, other.productId) && Objects.equals(linkId, other.linkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, linkId);
    }
}