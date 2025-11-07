package com.clinic.c46.AuthService.application.saga;

public enum EmailVerificationSagaStateMachine {
    PENDING_PATIENT_REPLY,
    TIMEOUT,
    COMPLETED,
    VALIDATE_OTP_FAILED,
}
