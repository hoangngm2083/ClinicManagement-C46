package com.clinic.c46.CommonService.exception;

public class TransientDataNotReadyException extends RuntimeException {
    public TransientDataNotReadyException(String message) {
        super(message);
    }
}
