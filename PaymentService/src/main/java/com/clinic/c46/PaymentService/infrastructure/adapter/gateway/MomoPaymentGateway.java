package com.clinic.c46.PaymentService.infrastructure.adapter.gateway;

import com.clinic.c46.PaymentService.application.service.PaymentGateway;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
public class MomoPaymentGateway implements PaymentGateway {
    @Override
    public void init(BigDecimal amount) {

    }

    @Override
    public void handleWebhook(Object payload) {

    }

    @Override
    public void refund(Object transactionId) {

    }
}
