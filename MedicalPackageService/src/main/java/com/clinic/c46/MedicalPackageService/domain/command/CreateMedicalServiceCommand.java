package com.clinic.c46.MedicalPackageService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.fasterxml.jackson.databind.JsonNode;

@Builder
public record CreateMedicalServiceCommand(@TargetAggregateIdentifier String medicalServiceId, String name,
        int processingPriority, String description, String departmentId,
        JsonNode formTemplate) {
}
