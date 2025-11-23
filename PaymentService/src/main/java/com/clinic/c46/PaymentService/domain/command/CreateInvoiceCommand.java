package com.clinic.c46.PaymentService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;


@Builder
public record CreateInvoiceCommand(@TargetAggregateIdentifier String invoiceId, String medicalFormId) {
}
