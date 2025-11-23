package com.clinic.c46.CommonService.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class BaseGlobalExceptionHandler {
    /* ====== GENERIC EXCEPTIONS ==================== */

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        Throwable root = unwrap(ex);
        if (root.getCause() instanceof ResourceNotFoundException) {
            return buildError(HttpStatus.NOT_FOUND, "Runtime Error", root.getMessage(), ex);
        }
        return buildError(HttpStatus.BAD_REQUEST, "Runtime Error", root.getMessage(), ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        Throwable root = unwrap(ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", root.getMessage(), ex);
    }

    /* ==================== UTILS ==================== */

    /**
     * Always retrieve the root cause (unwrap all nested/wrapper exceptions).
     */
    private Throwable unwrap(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error, String message, Exception ex) {
        log.error("{}: {}", error, message, ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .build();

        return ResponseEntity.status(status)
                .body(response);
    }

    /* ====== DTO ==================== */

    @lombok.Data
    @lombok.Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, String> details;
    }
}
