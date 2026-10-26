package ru.pet.project.api.dto;

import java.util.Map;

/**
 * Единый формат ошибки для фронтенда.
 * fieldErrors — ошибки по полям формы: {"email": "Неверный формат почты"}.
 */
public record ApiError(String message, Map<String, String> fieldErrors) {

    public ApiError(String message) {
        this(message, Map.of());
    }
}