package com.clinic.c46.CommonService.command.patient;


import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record DeletePatientCommand(@TargetAggregateIdentifier String patientId) {
}
