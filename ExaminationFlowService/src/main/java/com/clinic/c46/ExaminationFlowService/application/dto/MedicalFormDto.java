package com.clinic.c46.ExaminationFlowService.application.dto;


public record MedicalFormDto(
        String examinationId,
        PatientRepDto patient) {
}
