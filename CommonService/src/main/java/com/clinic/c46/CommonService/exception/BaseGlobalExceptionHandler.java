package com.clinic.c46.CommonService.exception;

import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.queryhandling.QueryExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

@RestControllerAdvice
public class BaseGlobalExceptionHandler {

    @ExceptionHandler(BaseDomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(BaseDomainException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getClass()
                .getSimpleName());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body);
    }

    @ExceptionHandler({QueryExecutionException.class, CompletionException.class})
    public ResponseEntity<?> handleQueryException(Exception ex) {
        Throwable cause = ex.getCause();
        String message = (cause != null ? cause.getMessage() : ex.getMessage());

        return ResponseEntity.badRequest()
                .body(Map.of("error", "Command failed", "message-global", message));
    }


    @ExceptionHandler(CommandExecutionException.class)
    public ResponseEntity<?> handleCommandExecution(CommandExecutionException ex) {
        Throwable cause = ex.getCause();
        String message = (cause != null ? cause.getMessage() : ex.getMessage());

        return ResponseEntity.badRequest()
                .body(Map.of("error", "Command failed", "message-global", message));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "InternalServerError");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach((error) -> {
                    String fieldName = error instanceof FieldError ?
                            ((FieldError) error).getField() : error.getObjectName();
                    String errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
