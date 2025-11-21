package com.clinic.c46.MedicalPackageService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;


@Builder
public record UpdateMedicalServiceInfoCommand(

        @TargetAggregateIdentifier String medicalServiceId, String name, String description, int processingPriority,
        String departmentId, String formTemplate) {
}
