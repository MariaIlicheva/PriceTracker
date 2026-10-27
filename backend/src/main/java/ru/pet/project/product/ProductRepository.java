package ru.pet.project.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Product> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ProductStatus status);
    Optional<Product> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndName(Long userId, String name);

    // Hibernate сам убирает дубли товаров, которые появляются из-за join со ссылками
    @Query("""
            select p from Product p
            left join fetch p.links pl
            left join fetch pl.link l
            left join fetch l.marketplace
            where p.user.id = :userId
            order by p.createdAt desc
            """)
    List<Product> findAllWithLinksByUserId(@Param("userId") Long userId);

    @Query("""
            select p from Product p
            left join fetch p.links pl
            left join fetch pl.link l
            left join fetch l.marketplace
            where p.id = :id and p.user.id = :userId
            """)
    Optional<Product> findWithLinksByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}