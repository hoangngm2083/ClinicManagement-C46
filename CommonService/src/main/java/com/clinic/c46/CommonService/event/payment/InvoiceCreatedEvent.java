package com.clinic.c46.CommonService.event.payment;

import lombok.Builder;

@Builder
public record InvoiceCreatedEvent(String invoiceId, String medicalFormId) {
}
