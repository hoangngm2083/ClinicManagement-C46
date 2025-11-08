package com.clinic.c46.CommonService.dto;
import lombok.Builder;

@Builder
public record PatientDto(String name, String email, String phone, String patientId
) {
}
