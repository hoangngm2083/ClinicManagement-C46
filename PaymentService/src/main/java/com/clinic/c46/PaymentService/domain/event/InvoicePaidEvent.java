package com.clinic.c46.PaymentService.domain.event;

import lombok.Builder;
import lombok.Value;

/**
 * Event fired when an invoice is marked as paid
 * This is triggered after successful transaction completion
 */
@Value
@Builder
public class InvoicePaidEvent {
    String invoiceId;
    String transactionId;
}
