package com.clinic.c46.CommonService.exception;

import org.axonframework.commandhandling.CommandExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

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


    @ExceptionHandler(CommandExecutionException.class)
    public ResponseEntity<?> handleCommandExecution(CommandExecutionException ex) {
        Throwable cause = ex.getCause();
        String message = (cause != null ? cause.getMessage() : ex.getMessage());

        return ResponseEntity.badRequest()
                .body(Map.of("error", "Command failed", "message", message));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "InternalServerError");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
