package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateMedicalServiceRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String departmentId;
    @NotBlank
    private int processingPriority;

    private JsonNode formTemplate;
}
