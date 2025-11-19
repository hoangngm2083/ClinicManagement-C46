package com.clinic.c46.ExaminationService.application.dto;

import lombok.Builder;

@Builder
public record MedicalResultViewDto(String doctorId, String serviceId, String data, String pdfUrl, String status,
                                   String doctorName) {
}
