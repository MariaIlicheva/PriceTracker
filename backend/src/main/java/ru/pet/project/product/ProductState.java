package ru.pet.project.product;

public enum ProductState {
    BELOW("below"),
    TRACKING("tracking"),
    PAUSED("paused");

    private final String value;

    ProductState(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}