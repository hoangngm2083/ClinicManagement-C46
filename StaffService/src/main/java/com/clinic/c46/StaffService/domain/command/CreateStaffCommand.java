package com.clinic.c46.StaffService.domain.command;

import com.clinic.c46.StaffService.domain.enums.Role;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;


@Builder
public record CreateStaffCommand(@TargetAggregateIdentifier String staffId, String name, String email, String phone,
                                 String description, String image, Role role, String eSignature, String departmentId) {

}
