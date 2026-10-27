package ru.pet.project.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pet.project.api.dto.MarketplaceResponse;
import ru.pet.project.marketplace.MarketplaceService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/marketplaces")
public class MarketplaceController {
    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @GetMapping
    public List<MarketplaceResponse> list() {
        return marketplaceService.list();
    }
}