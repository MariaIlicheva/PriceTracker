package ru.pet.project.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MarketplaceDomainRepository extends JpaRepository<MarketplaceDomain, String> {
    @Query("select d from MarketplaceDomain d join fetch d.marketplace")
    List<MarketplaceDomain> findAllWithMarketplace();
}