package com.clinic.c46.PaymentService.application.dto;

import lombok.Builder;

@Builder
public record ConfirmTransactionRequest(String transactionId, boolean isSuccess, String gatewayTransactionId) {
}
