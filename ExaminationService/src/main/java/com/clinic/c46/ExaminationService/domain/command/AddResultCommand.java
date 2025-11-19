package com.clinic.c46.ExaminationService.domain.command;

import com.clinic.c46.ExaminationService.domain.valueObject.MedicalResult;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record AddResultCommand(@TargetAggregateIdentifier String examId, MedicalResult medicalResult) {

}
