package com.clinic.c46.CommonService.event.medicalPackage;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record MedicalServiceCreatedEvent(String medicalServiceId, String name, String description,
        int processingPriority, String departmentId, JsonNode formTemplate) {
}
