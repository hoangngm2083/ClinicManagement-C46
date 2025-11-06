package com.clinic.c46.BookingService.application.saga;

public enum BookingProcessingStateMachine {
    LOCKED,
    PENDING_VERIFY_PATIENT_PHONE,
    PENDING_CREATE_PATIENT,
    PENDING_CREATE_APPOINTMENT,
    COMPLETED,
    FAILED,
    TIMEOUT
}
