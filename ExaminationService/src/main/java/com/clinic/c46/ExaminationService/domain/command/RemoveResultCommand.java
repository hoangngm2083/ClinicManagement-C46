package com.clinic.c46.ExaminationService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record RemoveResultCommand(@TargetAggregateIdentifier String examId, String serviceId) {

}
