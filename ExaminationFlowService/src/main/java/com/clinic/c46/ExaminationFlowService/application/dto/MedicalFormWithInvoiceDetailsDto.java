package com.clinic.c46.ExaminationFlowService.application.dto;

import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
import lombok.Builder;

import java.util.Optional;

/**
 * Medical form DTO with invoice details.
 * Used for RECEPTION_PAYMENT type queue items.
 */
@Builder
public record MedicalFormWithInvoiceDetailsDto(
                String id,
                Optional<InvoiceDetailsDto> invoice,
                String medicalFormStatus) implements MedicalFormDetailsBase {
}
