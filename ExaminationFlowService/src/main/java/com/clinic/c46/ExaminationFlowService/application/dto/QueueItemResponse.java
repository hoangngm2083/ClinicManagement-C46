package com.clinic.c46.ExaminationFlowService.application.dto;

import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
import lombok.Builder;

import java.util.Optional;

@Builder
public record QueueItemResponse(String queueItemId,
                Optional<MedicalFormDetailsBase> medicalForm,
                Optional<ServiceRepDto> requestedService,
                QueueItemType type
) {
}
