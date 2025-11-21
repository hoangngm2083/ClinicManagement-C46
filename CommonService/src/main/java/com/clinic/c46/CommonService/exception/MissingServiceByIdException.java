package com.clinic.c46.CommonService.exception;

public class MissingServiceByIdException extends RuntimeException {
    public MissingServiceByIdException(String message) {
        super(message);
    }
}
