package com.clinic.c46.StaffService.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Role {
    DOCTOR(0), RECEPTIONIST(1), MANAGER(2);
    private final int value;
}