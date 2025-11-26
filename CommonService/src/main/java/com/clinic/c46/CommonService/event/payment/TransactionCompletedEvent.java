package com.clinic.c46.CommonService.event.payment;

import lombok.Builder;


@Builder
public record TransactionCompletedEvent(String transactionId, String invoiceId, String gatewayTransactionId,
                                        String transactionStatus) {
}
