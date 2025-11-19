package com.clinic.c46.ExaminationFlowService.domain.command;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Set;

@Builder
public record CreateMedicalFormCommand(@TargetAggregateIdentifier @NotBlank String medicalFormId,
                                       @NotBlank String patientId, @NotBlank String invoiceId,
                                       @NotBlank String examinationId, @NotEmpty Set<String> packageIds) {
}
