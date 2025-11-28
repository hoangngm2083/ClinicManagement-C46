package com.clinic.c46.ExaminationFlowService.domain.command;

import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CreateQueueItemCommand(@TargetAggregateIdentifier @NotBlank String queueItemId,
        @NotBlank String medicalFormId, @NotBlank String serviceId,
        @NotBlank String queueId,
        @NotNull QueueItemType type) {
}
