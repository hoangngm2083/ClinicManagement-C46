package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto;

import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import lombok.Builder;

@Builder
public record QueueItemResponse(String queueItemId, MedicalFormDetailsDto medicalForm, ServiceRepDto requestedService) {
}
