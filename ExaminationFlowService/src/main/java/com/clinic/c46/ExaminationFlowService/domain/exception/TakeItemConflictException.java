package com.clinic.c46.ExaminationFlowService.domain.exception;

public class TakeItemConflictException extends IllegalStateException {
    public TakeItemConflictException(String message) {
        super(message);
    }
}
