package com.clinic.c46.PaymentService.infrastructure.adapter.payment;

import lombok.Builder;

import java.util.Map;

@Builder
public record WebhookResult(boolean success, boolean validSignature, String transactionId, String gatewayTransactionId,
                            Map<String, String> raw) {

    public static WebhookResult invalid(Map<String, String> raw) {
        return WebhookResult.builder()
                .validSignature(false)
                .success(false)
                .raw(raw)
                .build();
    }

    public static WebhookResult success(String txnId, String gatewayTxnId, boolean isSuccess, Map<String, String> raw) {
        return WebhookResult.builder()
                .validSignature(true)
                .success(isSuccess)
                .transactionId(txnId)
                .gatewayTransactionId(gatewayTxnId)
                .raw(raw)
                .build();
    }
}