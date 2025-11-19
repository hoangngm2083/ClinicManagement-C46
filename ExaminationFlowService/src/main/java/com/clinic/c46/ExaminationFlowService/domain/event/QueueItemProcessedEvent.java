package com.clinic.c46.ExaminationFlowService.domain.event;

public record QueueItemProcessedEvent(
        String queueItemId,
        String queueId,
        String serviceId
) {
}
