package com.clinic.c46.PaymentService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Command to mark an invoice as paid
 * Issued after successful transaction verification
 */
@Builder
public record MarkInvoicePaidCommand(@TargetAggregateIdentifier String invoiceId, String transactionId) {
}
