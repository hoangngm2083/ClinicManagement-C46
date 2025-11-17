package com.clinic.c46.ExaminationFlowService.domain.event;

import lombok.Builder;

@Builder
public record QueueItemCreatedEvent(String queueItemId, String medicalFormId, String serviceId, String queueId, String status) {
}
