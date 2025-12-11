package com.clinic.c46.CommonService.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ExaminationDto(
        String id,
        String patientId,
        String patientName,
        String patientEmail,
        String medicalFormId,
        List<?> results
) {
}
