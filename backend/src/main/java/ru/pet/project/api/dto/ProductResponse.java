package ru.pet.project.api.dto;

import ru.pet.project.product.Link;
import ru.pet.project.product.Product;
import ru.pet.project.product.ProductLink;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

// Карточка товара: всё из списка плюс ссылки с последними ценами
public record ProductResponse(Long id,
                              String name,
                              BigDecimal targetPrice,
                              String status,
                              String state,
                              BigDecimal bestPrice,
                              Instant createdAt,
                              List<LinkResponse> links) {

    public static ProductResponse from(Product product) {
        List<LinkResponse> links = product.getLinks().stream()
                .sorted(Comparator.comparing(ProductLink::getAddedAt))
                .map(productLink -> LinkResponse.from(productLink.getLink()))
                .toList();

        return new ProductResponse(product.getId(),
                product.getName(),
                product.getTargetPrice(),
                product.getStatus().dbValue(),
                product.state().value(),
                product.cheapestLink().map(Link::getLastPrice).orElse(null),
                product.getCreatedAt(),
                links);
    }
}