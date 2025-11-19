package com.clinic.c46.CommonService.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message + " không tồn tại!");
    }
}
