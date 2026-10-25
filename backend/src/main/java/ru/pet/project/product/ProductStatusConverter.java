package ru.pet.project.product;

import jakarta.persistence.Converter;
import ru.pet.project.common.DbEnumConverter;

@Converter
public class ProductStatusConverter extends DbEnumConverter<ProductStatus> {
    public ProductStatusConverter() {
        super(ProductStatus.class);
    }
}