package com.clinic.c46.ExaminationService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record UpdateResultStatusCommand(@TargetAggregateIdentifier String examId, String serviceId, String newStatus) {

}
