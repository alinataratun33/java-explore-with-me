package ru.practicum.main.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(Exception e) {
        log.warn("Некорректный запрос: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Bad request")
                .reason("Некорректный формат запроса")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Validation failed")
                .reason("Некорректные данные запроса")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException e) {
        log.warn("Конфликт: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Conflict")
                .reason(e.getMessage())
                .status(HttpStatus.CONFLICT.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.warn("Не найдено: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Not found")
                .reason(e.getMessage())
                .status(HttpStatus.NOT_FOUND.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbidden(ForbiddenException e) {
        log.warn("Доступ запрещён: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Forbidden")
                .reason(e.getMessage())
                .status(HttpStatus.FORBIDDEN.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Некорректный параметр: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Bad request")
                .reason(e.getMessage())
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleInternal(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Internal server error")
                .reason("Произошла непредвиденная ошибка")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());

        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Validation error")
                .reason("Некорректные данные запроса")
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now())
                .build();
    }
}