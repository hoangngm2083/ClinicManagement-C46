package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;


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
    private int processingPriority;
    @NotBlank
    private String formTemplate;
}

