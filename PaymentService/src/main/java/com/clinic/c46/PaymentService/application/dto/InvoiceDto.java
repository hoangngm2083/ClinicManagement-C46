package com.clinic.c46.PaymentService.application.dto;

import java.math.BigDecimal;
import java.util.Set;


public record InvoiceDto(String invoiceId, String patientId, Set<MedicalPackageRepDto> medicalPackages,
                         BigDecimal totalAmount, String status) {
}
