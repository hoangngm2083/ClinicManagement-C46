package com.clinic.c46.MedicalPackageService.application.dto;


import lombok.Builder;

@Builder
public record MedicalServiceDTO(String medicalServiceId, String name, int processingPriority, String description,
                                String departmentId, String departmentName, String formTemplate) {
}
