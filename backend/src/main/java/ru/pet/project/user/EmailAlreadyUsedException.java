package ru.pet.project.user;


public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("Почта уже зарегистрирована: " + email);
    }
}