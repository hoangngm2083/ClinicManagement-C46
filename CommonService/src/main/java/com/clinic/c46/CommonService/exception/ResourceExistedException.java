package com.clinic.c46.CommonService.exception;

public class ResourceExistedException extends RuntimeException {
    public ResourceExistedException(String message) {
        super(message + " đã tồn tại!");
    }
}
