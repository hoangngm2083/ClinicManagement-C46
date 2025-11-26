package com.clinic.c46.CommonService.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Invoice details DTO with patient information.
 * Contains all invoice data and related patient details.
 */
@Builder
public record InvoiceDetailsDto(
                String invoiceId,
                String patientId,
                String patientName,
                String patientEmail,
                String patientPhone,
                Set<MedicalPackageRepDto> medicalPackages,
                BigDecimal totalAmount,
                String status) {
}
