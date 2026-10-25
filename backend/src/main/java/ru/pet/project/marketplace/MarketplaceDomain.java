package ru.pet.project.marketplace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "marketplace_domains")
public class MarketplaceDomain {

    @Id
    @Column(name = "domain", length = 100)
    private String domain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marketplace_id", nullable = false)
    private Marketplace marketplace;

    protected MarketplaceDomain() {
    }

    public String getDomain() {
        return domain;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }
}