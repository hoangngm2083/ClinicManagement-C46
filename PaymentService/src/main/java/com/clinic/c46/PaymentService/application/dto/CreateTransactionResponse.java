package com.clinic.c46.PaymentService.application.dto;

import lombok.Builder;

@Builder
public record CreateTransactionResponse(String transactionId, String paymentUrl) {
}
