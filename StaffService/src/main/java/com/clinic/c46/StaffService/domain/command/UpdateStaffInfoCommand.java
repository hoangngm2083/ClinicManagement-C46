package com.clinic.c46.StaffService.domain.command;

import com.clinic.c46.StaffService.domain.enums.Role;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record UpdateStaffInfoCommand(@TargetAggregateIdentifier String staffId, String name, String phone,
                                     String description, String image, Role role, String eSignature,
                                     String departmentId) {

}