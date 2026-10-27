package ru.pet.project.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

// Тело запроса для создания и изменения товара
public record ProductRequest(

        @NotBlank(message = "Введите название товара")
        @Size(max = 255, message = "Название не длиннее 255 символов")
        String name,

        @NotNull(message = "Укажите пороговую цену")
        @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
        @Digits(integer = 10, fraction = 2, message = "Не больше 10 цифр до запятой и 2 после")
        BigDecimal targetPrice,

        @NotNull(message = "Добавьте хотя бы одну ссылку")
        @Size(min = 1, max = 10, message = "У товара должно быть от 1 до 10 ссылок")
        List<@NotBlank(message = "Ссылка не заполнена") String> links
) {
}