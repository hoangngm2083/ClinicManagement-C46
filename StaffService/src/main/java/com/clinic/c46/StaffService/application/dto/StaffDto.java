package com.clinic.c46.StaffService.application.dto;

import lombok.Builder;

@Builder
public record StaffDto(

        String id,

        String name,

        String email,

        String phone,

        String description,

        String image,

        int role,

        String eSignature,

        String departmentId) {
}
