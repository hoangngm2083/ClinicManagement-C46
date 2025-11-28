package com.clinic.c46.PaymentService.application.query;

import com.clinic.c46.PaymentService.domain.aggregate.TransactionStatus;
import lombok.Builder;

import java.util.Set;

@Builder
public record GetAllTransactionsQuery(
        String invoiceId,
        Set<TransactionStatus> statuses
) {
}
