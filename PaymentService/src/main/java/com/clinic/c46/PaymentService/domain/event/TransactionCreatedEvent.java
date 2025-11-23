package com.clinic.c46.PaymentService.domain.event;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransactionCreatedEvent(String transactionId, String invoiceId, BigDecimal amount, String staffId,
                                      String paymentMethod) {
}
