package com.clinic.c46.PaymentService.domain.aggregate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum InvoiceStatus {
    PENDING_PAYMENT(0), PAYED(1);
    private final int value;
}
