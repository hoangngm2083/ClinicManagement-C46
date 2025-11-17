package com.clinic.c46.ExaminationFlowService.application.service.medicalForm.dto;


import java.util.Set;


public record CreateMedicalFormDto(
        String patientId,
        Set<String> medicalPackageIds
) {
}