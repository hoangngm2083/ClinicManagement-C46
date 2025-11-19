package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.controller.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Set;

public record CreateMedicalFormRequest(@NotBlank String patientId, @NotEmpty Set<String> medicalPackageIds) {
}
