package com.clinic.c46.ExaminationFlowService.domain.event;

import lombok.Builder;

@Builder
public record QueueItemTakenEvent(String queueItemId, String staffId, String queueId) {
}
