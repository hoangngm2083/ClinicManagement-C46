package com.clinic.c46.CommonService.command.payment;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

@Builder
public record CreateInvoiceCommand(@TargetAggregateIdentifier String invoiceId, String medicalFormId, BigDecimal snapshotPrice) {
}
