package ru.pet.project.parsing;

import java.math.BigDecimal;
// Получает текущую цену товара по ссылке
// У каждого способа парсинга своя реализация
public interface PriceParser {
    BigDecimal fetchPrice(ParseTarget target) throws ParseFailedException;
}