package com.clinic.c46.ExaminationFlowService.domain.event;

import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
import lombok.Builder;

@Builder
public record QueueItemTakenEvent(String queueItemId, String staffId, String queueId, QueueItemType type) {
}
