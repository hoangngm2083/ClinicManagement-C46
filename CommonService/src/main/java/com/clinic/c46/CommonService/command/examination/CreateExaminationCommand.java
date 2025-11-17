package com.clinic.c46.CommonService.command.examination;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CreateExaminationCommand(@TargetAggregateIdentifier @NotBlank String examinationId,
                                       @NotBlank String patientId) {

}
