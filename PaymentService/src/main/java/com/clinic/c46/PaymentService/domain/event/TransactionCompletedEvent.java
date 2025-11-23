package com.clinic.c46.PaymentService.domain.event;

import lombok.Builder;


@Builder
public record TransactionCompletedEvent(String transactionId, String invoiceId, String gatewayTransactionId,
                                        String transactionStatus) {
}
