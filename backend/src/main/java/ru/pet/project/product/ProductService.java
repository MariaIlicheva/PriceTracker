package ru.pet.project.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.pet.project.api.dto.ProductRequest;
import ru.pet.project.api.dto.ProductResponse;
import ru.pet.project.api.dto.ProductSummaryResponse;
import ru.pet.project.marketplace.MarketplaceService;
import ru.pet.project.marketplace.MarketplaceService.ResolvedLink;
import ru.pet.project.user.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final MarketplaceService marketplaceService;

    public ProductService(ProductRepository productRepository,
                          LinkRepository linkRepository,
                          UserRepository userRepository,
                          MarketplaceService marketplaceService) {
        this.productRepository = productRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.marketplaceService = marketplaceService;
    }

    // Сначала товары с ценой ниже порога, внутри групп — новые сверху (порядок из запроса)
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> list(Long userId) {
        return productRepository.findAllWithLinksByUserId(userId).stream()
                .sorted(Comparator.comparing(product -> product.state() != ProductState.BELOW))
                .map(ProductSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long userId, Long productId) {
        return ProductResponse.from(findOwned(userId, productId));
    }

    public ProductResponse create(Long userId, ProductRequest request) {
        String name = request.name().trim();
        if (productRepository.existsByUserIdAndName(userId, name)) {
            throw new ProductNameTakenException();
        }
        List<ResolvedLink> links = marketplaceService.resolveAll(request.links());

        Product product = new Product(userRepository.getReferenceById(userId), name, request.targetPrice());
        for (ResolvedLink resolved : links) {
            product.addLink(findOrCreateLink(resolved));
        }
        productRepository.save(product);
        return ProductResponse.from(product);
    }

    public ProductResponse update(Long userId, Long productId, ProductRequest request) {
        Product product = findOwned(userId, productId);

        String name = request.name().trim();
        if (!name.equals(product.getName()) && productRepository.existsByUserIdAndName(userId, name)) {
            throw new ProductNameTakenException();
        }
        List<ResolvedLink> resolved = marketplaceService.resolveAll(request.links());

        Set<String> newUrls = resolved.stream()
                .map(ResolvedLink::url)
                .collect(Collectors.toSet());
        List<Link> removed = product.getLinks().stream()
                .map(ProductLink::getLink)
                .filter(link -> !newUrls.contains(link.getUrl()))
                .toList();
        removed.forEach(product::removeLink);

        Set<String> currentUrls = product.getLinks().stream()
                .map(productLink -> productLink.getLink().getUrl())
                .collect(Collectors.toSet());
        for (ResolvedLink link : resolved) {
            if (!currentUrls.contains(link.url())) {
                product.addLink(findOrCreateLink(link));
            }
        }

        product.setName(name);
        product.setTargetPrice(request.targetPrice());
        // save не нужен: сущность загружена в этой транзакции
        // Hibernate сам запишет изменения
        return ProductResponse.from(product);
    }

    public ProductResponse changeStatus(Long userId, Long productId, String status) {
        Product product = findOwned(userId, productId);
        if (ProductStatus.PAUSED.dbValue().equals(status)) {
            product.pause();
        } else {
            product.resume();
        }
        return ProductResponse.from(product);
    }

    public void delete(Long userId, Long productId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        productRepository.delete(product);
    }

    private Product findOwned(Long userId, Long productId) {
        return productRepository.findWithLinksByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Link findOrCreateLink(ResolvedLink resolved) {
        return linkRepository.findByUrl(resolved.url())
                .orElseGet(() -> linkRepository.save(new Link(resolved.marketplace(), resolved.url())));
    }
}