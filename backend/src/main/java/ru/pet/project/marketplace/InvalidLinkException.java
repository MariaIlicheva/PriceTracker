package ru.pet.project.marketplace;

//Ссылку нельзя отслеживать: это не ссылка или магазин не поддерживается
public class InvalidLinkException extends RuntimeException {
    private final int index; //номер ссылки в форме, чтобы фронтенд подсветил нужное поле

    public InvalidLinkException(int index, String message) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}