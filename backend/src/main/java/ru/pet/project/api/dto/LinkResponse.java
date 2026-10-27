package ru.pet.project.api.dto;

import ru.pet.project.product.Link;
import java.math.BigDecimal;
import java.time.Instant;


public record LinkResponse(Long id,
                           String marketplaceCode,
                           String marketplaceName,
                           String url,
                           BigDecimal lastPrice,
                           Instant lastCheckedAt) {

    public static LinkResponse from(Link link) {
        return new LinkResponse(link.getId(),
                link.getMarketplace().getCode(),
                link.getMarketplace().getName(),
                link.getUrl(),
                link.getLastPrice(),
                link.getLastCheckedAt());
    }
}