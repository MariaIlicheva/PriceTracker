package ru.pet.project.product;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Product> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ProductStatus status);
    Optional<Product> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndName(Long userId, String name);
}