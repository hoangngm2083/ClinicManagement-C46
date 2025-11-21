package com.clinic.c46.StaffService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record DeleteDepartmentCommand(@TargetAggregateIdentifier String departmentId) {

}
