package com.nbbang.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid(IllegalArgumentException e) { return Map.of("message", e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty() ? "입력값을 확인해 주세요."
                : e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        return Map.of("message", message);
    }
}

