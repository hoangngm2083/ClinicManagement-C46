package com.clinic.c46.CommonService.command.examination;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CompleteExaminationCommand(@TargetAggregateIdentifier String examinationId) {
}
