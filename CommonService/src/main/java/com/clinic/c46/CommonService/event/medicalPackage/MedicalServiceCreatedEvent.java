package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

import com.fasterxml.jackson.databind.JsonNode;

@Builder
public record MedicalServiceCreatedEvent(String medicalServiceId, String name, String description,
        int processingPriority, String departmentId, JsonNode formTemplate) {
}
