package com.clinic.c46.StaffService.infrastructure.adapter.web.dto;


import lombok.Builder;

@Builder
public record CreateDepartmentRequest(String name, String description) {
}
