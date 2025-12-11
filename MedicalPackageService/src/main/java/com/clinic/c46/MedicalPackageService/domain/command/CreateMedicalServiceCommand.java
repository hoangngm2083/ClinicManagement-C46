package com.clinic.c46.MedicalPackageService.domain.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CreateMedicalServiceCommand(@TargetAggregateIdentifier String medicalServiceId, String name,
        int processingPriority, String description, String departmentId,
        JsonNode formTemplate) {
}
