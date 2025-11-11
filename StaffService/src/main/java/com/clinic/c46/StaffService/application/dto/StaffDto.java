package com.clinic.c46.StaffService.application.dto;

import com.clinic.c46.StaffService.domain.enums.Role;

public record StaffDto(

        String id,

        String name,

        String email,

        String phone,

        String description,

        String image,

        Role role,

        String eSignature,

        String departmentId) {
}
