package com.clinic.c46.NotificationService.infrastructure.adapter.exception;

public class DataNotFoundRetryableException extends RuntimeException {
    public DataNotFoundRetryableException(String message) {
        super(message);
    }

    public DataNotFoundRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
