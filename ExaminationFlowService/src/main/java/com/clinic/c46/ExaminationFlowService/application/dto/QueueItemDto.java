package com.clinic.c46.ExaminationFlowService.application.dto;

import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;

public record QueueItemDto(String queueItemId, String medicalFormId, String serviceId, QueueItemType type) {
}
