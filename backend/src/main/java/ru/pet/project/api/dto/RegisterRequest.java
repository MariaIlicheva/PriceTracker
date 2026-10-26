package ru.pet.project.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record RegisterRequest(
        @NotBlank(message = "Введите почту")
        @Email(message = "Неверный формат почты")
        @Size(max = 255, message = "Почта не длиннее 255 символов")
        String email,

        // bcrypt учитывает только первые 72 байта пароля, поэтому длиннее не разрешаем.
        @NotBlank(message = "Введите пароль")
        @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$", message = "Пароль должен содержать буквы и цифры")
        String password
) {
}