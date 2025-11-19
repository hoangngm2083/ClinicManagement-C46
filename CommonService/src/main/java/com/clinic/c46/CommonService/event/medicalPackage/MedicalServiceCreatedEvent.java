package com.clinic.c46.CommonService.event.medicalPackage;


import lombok.Builder;

@Builder
public record MedicalServiceCreatedEvent(String medicalServiceId, String name, String description,
                                         int processingPriority, String departmentId, String formTemplate) {
}
