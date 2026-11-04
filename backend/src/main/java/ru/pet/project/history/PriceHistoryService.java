package ru.pet.project.history;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pet.project.api.dto.PriceHistoryResponse;
import ru.pet.project.api.dto.PriceHistoryResponse.Point;
import ru.pet.project.api.dto.PriceHistoryResponse.Series;
import ru.pet.project.product.Link;
import ru.pet.project.product.Product;
import ru.pet.project.product.ProductLink;
import ru.pet.project.product.ProductNotFoundException;
import ru.pet.project.product.ProductRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PriceHistoryService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistoryService(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional(readOnly = true)
    public PriceHistoryResponse getHistory(Long userId, Long productId, String periodValue) {
        HistoryPeriod period = HistoryPeriod.fromValue(periodValue);

        // поиск товара вместе с владельцем
        Product product = productRepository.findWithLinksByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Instant to = Instant.now();
        Instant from = to.minus(period.length());

        // ссылки в том же порядке как в карточке товара
        List<Link> links = product.getLinks().stream()
                .sorted(Comparator.comparing(ProductLink::getAddedAt))
                .map(ProductLink::getLink)
                .toList();
        List<Long> linkIds = links.stream().map(Link::getId).toList();

        // Один запрос на все ссылки сразу.
        // Условие по captured_at позволяет субд читать только секции за нужные дни (partition pruning)
        Map<Long, List<Point>> pointsByLink = priceHistoryRepository
                .findByLinkIdInAndCapturedAtBetweenOrderByCapturedAt(linkIds, from, to).stream()
                .collect(Collectors.groupingBy(
                        PriceHistory::getLinkId,
                        Collectors.mapping(h -> new Point(h.getCapturedAt(), h.getPrice()), Collectors.toList())));

        List<Series> series = links.stream()
                .map(link -> new Series(
                        link.getId(),
                        link.getMarketplace().getCode(),
                        link.getMarketplace().getName(),
                        pointsByLink.getOrDefault(link.getId(), List.of())))
                .toList();

        return new PriceHistoryResponse(period.value(), from, to, series);
    }
}