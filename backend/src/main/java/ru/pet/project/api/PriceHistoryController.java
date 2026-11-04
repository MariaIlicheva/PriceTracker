package ru.pet.project.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.pet.project.api.dto.PriceHistoryResponse;
import ru.pet.project.history.PriceHistoryService;
import ru.pet.project.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/products")
public class PriceHistoryController {
    private final PriceHistoryService priceHistoryService;

    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping("/{id}/history")
    public PriceHistoryResponse history(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable("id") Long id,
                                        @RequestParam(name = "period", defaultValue = "week") String period) {
        return priceHistoryService.getHistory(user.getId(), id, period);
    }
}