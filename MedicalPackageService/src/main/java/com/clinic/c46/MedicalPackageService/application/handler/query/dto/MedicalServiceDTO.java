package com.clinic.c46.MedicalPackageService.application.handler.query.dto;


import lombok.Builder;

@Builder
public record MedicalServiceDTO(String medicalServiceId, String name, String description) {
}
