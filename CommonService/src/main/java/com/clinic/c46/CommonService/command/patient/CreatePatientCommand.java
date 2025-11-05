package com.clinic.c46.CommonService.command.patient;


import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CreatePatientCommand(@TargetAggregateIdentifier String patientId, String name, String phone,
                                   String email) {
}
