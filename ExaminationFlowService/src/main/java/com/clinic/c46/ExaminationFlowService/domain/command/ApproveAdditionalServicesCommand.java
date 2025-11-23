package com.clinic.c46.ExaminationFlowService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Set;

@Builder
public record ApproveAdditionalServicesCommand(@TargetAggregateIdentifier String medicalFormId,
                                               Set<String> additionalServiceIds) {
}
