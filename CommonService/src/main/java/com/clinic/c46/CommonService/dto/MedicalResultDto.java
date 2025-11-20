package com.clinic.c46.CommonService.dto;

import lombok.Builder;

@Builder
public record MedicalResultDto(String doctorId, String serviceId, String data, String pdfUrl, String status,
                               String doctorName) {
}
