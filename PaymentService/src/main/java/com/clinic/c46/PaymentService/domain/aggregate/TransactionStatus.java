package com.clinic.c46.PaymentService.domain.aggregate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransactionStatus {
    PENDING(0), COMPLETED(1), FAILED(2);
    private final int value;
}
