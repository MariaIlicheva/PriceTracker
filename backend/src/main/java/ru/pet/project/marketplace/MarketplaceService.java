package ru.pet.project.marketplace;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pet.project.api.dto.MarketplaceResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {
    private final MarketplaceDomainRepository domainRepository;

    public MarketplaceService(MarketplaceDomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    // ссылка после проверки
    // нормализованный адрес и магазин, которому она принадлежит
    public record ResolvedLink(String url, Marketplace marketplace) {
    }


    // Проверяет все ссылки из формы, порядок сохраняется
    @Transactional(readOnly = true)
    public List<ResolvedLink> resolveAll(List<String> rawUrls) {
        List<MarketplaceDomain> domains = domainRepository.findAllWithMarketplace();
        Set<String> seen = new HashSet<>();
        List<ResolvedLink> result = new ArrayList<>();

        for (int i = 0; i < rawUrls.size(); i++) {
            int index = i;
            UrlNormalizer.NormalizedUrl normalized;
            try {
                normalized = UrlNormalizer.normalize(rawUrls.get(i));
            } catch (IllegalArgumentException e) {
                throw new InvalidLinkException(index, e.getMessage());
            }

            Marketplace marketplace = findMarketplace(domains, normalized.host())
                    .orElseThrow(() -> new InvalidLinkException(index,
                            "Этот магазин не поддерживается. Подходят Wildberries, Ozon, Яндекс Маркет, Купер и AliExpress"));

            // Две разные записи в форме могут оказаться одной ссылкой после нормализации
            if (!seen.add(normalized.url())) {
                throw new InvalidLinkException(index, "Эта ссылка уже есть в списке");
            }
            result.add(new ResolvedLink(normalized.url(), marketplace));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MarketplaceResponse> list() {
        Map<Marketplace, List<String>> byMarketplace = domainRepository.findAllWithMarketplace().stream()
                .sorted(Comparator.comparing((MarketplaceDomain d) -> d.getMarketplace().getId())
                        .thenComparing(MarketplaceDomain::getDomain))
                .collect(Collectors.groupingBy(MarketplaceDomain::getMarketplace, LinkedHashMap::new,
                        Collectors.mapping(MarketplaceDomain::getDomain, Collectors.toList())));

        return byMarketplace.entrySet().stream()
                .map(e -> new MarketplaceResponse(e.getKey().getCode(), e.getKey().getName(), e.getValue()))
                .toList();
    }

    private static Optional<Marketplace> findMarketplace(List<MarketplaceDomain> domains, String host) {
        return domains.stream()
                .filter(d -> host.equals(d.getDomain()) || host.endsWith("." + d.getDomain()))
                .map(MarketplaceDomain::getMarketplace)
                .findFirst();
    }
}