package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

@Builder
public record MedicalServiceInfoUpdatedEvent(

        String medicalServiceId,
        String name,
        String description,
        String departmentId
) {
}
