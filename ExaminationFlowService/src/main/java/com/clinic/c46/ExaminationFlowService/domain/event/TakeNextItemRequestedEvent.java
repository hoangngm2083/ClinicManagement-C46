package com.clinic.c46.ExaminationFlowService.domain.event;

import lombok.Builder;

@Builder
public record TakeNextItemRequestedEvent(String staffId, String queueId) {
}
