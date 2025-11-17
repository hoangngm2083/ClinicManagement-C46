package com.clinic.c46.ExaminationService.infrastructure.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateMedicalResultRequest(@NotBlank String medicalFormId, @NotBlank String serviceId,
                                         @NotBlank String data) {

}
