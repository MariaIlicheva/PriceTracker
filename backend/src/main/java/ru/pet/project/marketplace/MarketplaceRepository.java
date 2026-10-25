package ru.pet.project.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MarketplaceRepository extends JpaRepository<Marketplace, Short> {
    Optional<Marketplace> findByCode(String code);
}