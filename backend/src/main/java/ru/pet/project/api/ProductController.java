package ru.pet.project.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.pet.project.api.dto.ProductRequest;
import ru.pet.project.api.dto.ProductResponse;
import ru.pet.project.api.dto.ProductSummaryResponse;
import ru.pet.project.api.dto.StatusRequest;
import ru.pet.project.product.ProductService;
import ru.pet.project.security.AuthenticatedUser;
import java.util.List;

// Товары текущего пользователя
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return productService.list(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.create(user.getId(), request);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                               @PathVariable("id") Long id) {
        return productService.get(user.getId(), id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody ProductRequest request) {
        return productService.update(user.getId(), id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductResponse changeStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable("id") Long id,
                                        @Valid @RequestBody StatusRequest request) {
        return productService.changeStatus(user.getId(), id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable("id") Long id) {
        productService.delete(user.getId(), id);
    }
}