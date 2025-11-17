package com.clinic.c46.ExaminationFlowService.domain.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record TakeNextItemCommand(@NotBlank @TargetAggregateIdentifier String queueItemId, @NotBlank String staffId) {
}
