package ru.pet.project.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StatusRequest(
        @NotNull(message = "Укажите статус")
        @Pattern(regexp = "active|paused", message = "Статус может быть active или paused")
        String status
) {
}