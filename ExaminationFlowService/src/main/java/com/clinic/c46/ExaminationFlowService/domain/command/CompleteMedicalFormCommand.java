package com.clinic.c46.ExaminationFlowService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CompleteMedicalFormCommand(
        @TargetAggregateIdentifier String medicalFormId) {
}
