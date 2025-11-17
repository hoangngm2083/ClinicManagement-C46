package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto;

import java.util.Set;

public record RequestAdditionalServicesRequest(String medicalFormId, Set<String> additionalServiceIds) {
}
