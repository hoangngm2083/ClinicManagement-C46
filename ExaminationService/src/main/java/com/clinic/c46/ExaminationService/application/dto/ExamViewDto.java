package com.clinic.c46.ExaminationService.application.dto;

import lombok.Builder;

@Builder
public record ExamViewDto(String id, String patientId, String medicalFormId, String patientName, String patientEmail) {

}
