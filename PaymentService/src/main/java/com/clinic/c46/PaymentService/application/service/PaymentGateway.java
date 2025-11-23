package com.clinic.c46.PaymentService.application.service;

import java.math.BigDecimal;

public interface PaymentGateway {
    void init(BigDecimal amount);

    void handleWebhook(Object payload); // compare payment result to payment requested

    void refund(Object transactionId);
}
