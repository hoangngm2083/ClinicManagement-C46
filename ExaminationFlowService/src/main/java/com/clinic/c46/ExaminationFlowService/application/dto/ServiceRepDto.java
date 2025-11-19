package com.clinic.c46.ExaminationFlowService.application.dto;

import lombok.Builder;

@Builder
public record ServiceRepDto(String serviceId, String name, int processingPriority, String departmentId,
                            String formTemplate) {
}
