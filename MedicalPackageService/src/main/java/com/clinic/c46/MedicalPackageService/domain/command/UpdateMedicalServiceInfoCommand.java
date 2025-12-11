package com.clinic.c46.MedicalPackageService.domain.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record UpdateMedicalServiceInfoCommand(

                @TargetAggregateIdentifier String medicalServiceId, String name, String description,
                int processingPriority,
                String departmentId, JsonNode formTemplate) {
}
