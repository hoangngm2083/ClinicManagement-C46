package com.clinic.c46.ExaminationFlowService.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record ServiceRepDto(String serviceId, String name, int processingPriority, String departmentId,
        JsonNode formTemplate) implements Comparable<ServiceRepDto> {
    @Override
    public int compareTo(ServiceRepDto other) {

        return Integer.compare(this.processingPriority, other.processingPriority);

    }
}