package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record RequestAdditionalServicesRequest(String queueItemId, Set<String> additionalServiceIds) {
}
