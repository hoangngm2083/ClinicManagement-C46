package com.clinic.c46.MedicalPackageService.domain.event;

import com.clinic.c46.MedicalPackageService.domain.view.DepartmentView;
import lombok.Builder;

@Builder
public record MedicalServiceInfoUpdatedEvent(

        String medicalServiceId,
        String name,
        String description,
        String departmentId
) {
}
