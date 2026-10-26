package ru.pet.project.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.pet.project.api.dto.ApiError;
import ru.pet.project.user.EmailAlreadyUsedException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Превращает исключения в понятные ответы с кодом и JSON вместо стектрейса. */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Не прошла проверка @Valid: 400 и ошибки по каждому полю. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return new ApiError("Проверьте введённые данные", fieldErrors);
    }

    /** Тело запроса — не JSON или JSON с ошибкой. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException e) {
        return new ApiError("Некорректное тело запроса");
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailUsed(EmailAlreadyUsedException e) {
        return new ApiError("Этот адрес уже зарегистрирован. Войдите или используйте другой",
                Map.of("email", "Адрес уже зарегистрирован"));
    }
}