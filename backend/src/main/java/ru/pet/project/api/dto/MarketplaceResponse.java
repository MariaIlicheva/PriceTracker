package ru.pet.project.api.dto;

import java.util.List;

public record MarketplaceResponse(String code, String name, List<String> domains) {
}