package ru.pet.project.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
// История цен товара для графика: по одному ряду точек на каждую ссылку

public record PriceHistoryResponse(String period,
                                   Instant from,
                                   Instant to,
                                   List<Series> series) {

    // линия графика
    public record Series(Long linkId,
                         String marketplaceCode,
                         String marketplaceName,
                         List<Point> points) {
    }

    // один замер цены
    public record Point(Instant capturedAt, BigDecimal price) {
    }
}