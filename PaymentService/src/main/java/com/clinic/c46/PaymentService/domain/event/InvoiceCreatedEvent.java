package com.clinic.c46.PaymentService.domain.event;

import lombok.Builder;


@Builder
public record InvoiceCreatedEvent(String invoiceId, String medicalFormId) {
}
