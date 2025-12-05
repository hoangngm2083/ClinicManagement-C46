package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class UpdateMedicalServiceRequest {
    private String name;
    private String description;
    private String departmentId;
    private Integer processingPriority;
    private JsonNode formTemplate;
}
