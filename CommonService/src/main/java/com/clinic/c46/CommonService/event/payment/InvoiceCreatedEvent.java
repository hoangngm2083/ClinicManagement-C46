package com.clinic.c46.CommonService.event.payment;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record InvoiceCreatedEvent(String invoiceId, String medicalFormId, BigDecimal snapshotPrice) {
}
