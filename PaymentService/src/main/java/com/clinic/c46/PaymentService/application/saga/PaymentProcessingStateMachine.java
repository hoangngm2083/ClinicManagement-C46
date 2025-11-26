package com.clinic.c46.PaymentService.application.saga;

public enum PaymentProcessingStateMachine {
    TRANS_CREATED, PENDING_PAYMENT, PENDING_CREATE_INVOICE, PENDING_SEND_INVOICE_TO_PATIENT, COMPLETED, CANCELLED,
}
