package com.clinic.c46.ExaminationFlowService.application.dto;

import lombok.Builder;

import java.util.Optional;

@Builder
public record QueueItemResponse(String queueItemId, Optional<MedicalFormDetailsDto> medicalForm,
                                Optional<ServiceRepDto> requestedService) {
}
