package com.clinic.c46.BookingService.application.saga.BookingProcessing;

public enum BookingProcessingStateMachine {
    LOCKED, PENDING_VERIFY_PATIENT_EMAIL, PENDING_CREATE_PATIENT, PENDING_CREATE_APPOINTMENT, PENDING_RELEASE_SLOT_LOCKED, COMPLETED, FAILED, TIMEOUT
}
