package ru.pet.project.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.pet.project.api.dto.ApiError;
import ru.pet.project.marketplace.InvalidLinkException;
import ru.pet.project.product.ProductNameTakenException;
import ru.pet.project.product.ProductNotFoundException;
import ru.pet.project.user.EmailAlreadyUsedException;

import java.util.LinkedHashMap;
import java.util.Map;

// Превращает исключения в понятные ответы с кодом и JSON
@RestControllerAdvice
public class ApiExceptionHandler {

    // Не прошла проверка @Valid: 400 и ошибки по каждому полю
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return new ApiError("Проверьте введённые данные", fieldErrors);
    }

    // Тело запроса — не JSON или JSON с ошибкой
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
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleProductNotFound(ProductNotFoundException e) {
        return new ApiError("Товар не найден");
    }

    @ExceptionHandler(ProductNameTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleProductNameTaken(ProductNameTakenException e) {
        return new ApiError("У вас уже есть товар с таким названием",
                Map.of("name", "Название уже используется"));
    }

    @ExceptionHandler(InvalidLinkException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidLink(InvalidLinkException e) {
        return new ApiError("Проверьте ссылки",
                Map.of("links[" + e.getIndex() + "]", e.getMessage()));
    }

    // products/abc вместо числа
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return new ApiError("Некорректный параметр запроса");
    }

    //Редкая гонка: два запроса одновременно добавили одну и ту же новую ссылку или одинаковое название
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataConflict(DataIntegrityViolationException e) {
        return new ApiError("Данные изменились одновременно с другим запросом. Повторите попытку");
    }
}