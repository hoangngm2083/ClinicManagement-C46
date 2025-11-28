package com.clinic.c46.PaymentService.application.dto;

import lombok.Builder;

@Builder

public record TransactionStatusDto(String transactionId, String invoiceId, String status) {
}
