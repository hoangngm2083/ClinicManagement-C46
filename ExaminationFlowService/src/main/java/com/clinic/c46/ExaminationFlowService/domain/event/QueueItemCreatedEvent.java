package com.clinic.c46.ExaminationFlowService.domain.event;

import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
import lombok.Builder;

@Builder
public record QueueItemCreatedEvent(String queueItemId,
        String medicalFormId,
        String serviceId,
        String queueId,
        String status,
        QueueItemType type) {
}
