package ru.pet.project.api.dto;

import ru.pet.project.product.Link;
import ru.pet.project.product.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

// Строка в списке товаров на главном экране
public record ProductSummaryResponse(Long id,
                                     String name,
                                     BigDecimal targetPrice,
                                     String status,
                                     String state,
                                     BigDecimal bestPrice,
                                     String bestMarketplace,
                                     int linkCount,
                                     Instant createdAt) {

    public static ProductSummaryResponse from(Product product) {
        Optional<Link> cheapest = product.cheapestLink();
        return new ProductSummaryResponse(product.getId(),
                product.getName(),
                product.getTargetPrice(),
                product.getStatus().dbValue(),
                product.state().value(),
                cheapest.map(Link::getLastPrice).orElse(null),
                cheapest.map(link -> link.getMarketplace().getName()).orElse(null),
                product.getLinks().size(),
                product.getCreatedAt());
    }
}