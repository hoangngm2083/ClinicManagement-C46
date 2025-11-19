package com.clinic.c46.ExaminationFlowService.application.dto;

import lombok.Builder;

@Builder
public record PatientRepDto(String name, String email, String phone, String patientId) {
}
