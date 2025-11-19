package com.clinic.c46.ExaminationService.application.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record ExamViewDto(String id, String patientId, String medicalFormId, String patientName, String patientEmail,
                          Set<MedicalResultViewDto> results) {

}
