package ru.pet.project.product;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Товар " + id + " не найден");
    }
}