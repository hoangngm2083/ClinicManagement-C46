package com.clinic.c46.MedicalPackageService.application.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record MedicalServiceDetailsDTO(String medicalServiceId, String name, int processingPriority, String description,
                                       String departmentId, String departmentName, JsonNode formTemplate) {
}
