package com.clinic.c46.StaffService.domain.event;

import com.clinic.c46.StaffService.domain.enums.Role;
import lombok.Builder;


@Builder
public record StaffCreatedEvent(String staffId, String name, String email, String phone, String description,
                                String image, Role role, String eSignature, String departmentId,
                                String accountName, String password) {
}
