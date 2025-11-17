package com.clinic.c46.CommonService.query.patient;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ExistsPatientByIdQuery(@NotBlank String patientId) {
}
