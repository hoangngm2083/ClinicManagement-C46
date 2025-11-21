package com.clinic.c46.ExaminationFlowService.domain.event;

public record QueueItemCompletedEvent(String queueItemId, String staffId, String serviceId) {
}
