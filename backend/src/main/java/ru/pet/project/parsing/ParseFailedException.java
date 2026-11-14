package ru.pet.project.parsing;

public class ParseFailedException extends Exception {
    public ParseFailedException(String message) {
        super(message);
    }
    public ParseFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}