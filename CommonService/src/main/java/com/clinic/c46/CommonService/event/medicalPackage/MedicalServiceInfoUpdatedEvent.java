package com.clinic.c46.CommonService.event.medicalPackage;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record MedicalServiceInfoUpdatedEvent(JsonNode formTemplate, String medicalServiceId, String name,
        int processingPriority, String description, String departmentId) {
}
