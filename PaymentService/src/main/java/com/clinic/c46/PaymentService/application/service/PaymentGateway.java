package com.clinic.c46.PaymentService.application.service;

import com.clinic.c46.PaymentService.infrastructure.adapter.payment.WebhookResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PaymentGateway {

    String generateURL(String transactionId, BigDecimal amount, String clientIp);

    public CompletableFuture<WebhookResult> handleWebhook(
            Map<String, String> params);// compare payment result to payment requested

    void refund(Object transactionId);
}
