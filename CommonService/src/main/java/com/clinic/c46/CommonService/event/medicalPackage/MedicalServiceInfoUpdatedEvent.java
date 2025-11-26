package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

import com.fasterxml.jackson.databind.JsonNode;

@Builder
public record MedicalServiceInfoUpdatedEvent(JsonNode formTemplate, String medicalServiceId, String name,
        int processingPriority, String description, String departmentId) {
}
