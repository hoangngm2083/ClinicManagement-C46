package com.clinic.c46.PaymentService.application.query;

import lombok.Builder;

@Builder
public record ExistsTransactionOfInvoiceQuery(String invoiceId) {
}
