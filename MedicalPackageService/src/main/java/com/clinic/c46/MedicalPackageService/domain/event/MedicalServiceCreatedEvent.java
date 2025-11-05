package com.clinic.c46.MedicalPackageService.domain.event;


import lombok.Builder;

@Builder
public record MedicalServiceCreatedEvent(String medicalServiceId, String name, String description, String departmentId) {
}
