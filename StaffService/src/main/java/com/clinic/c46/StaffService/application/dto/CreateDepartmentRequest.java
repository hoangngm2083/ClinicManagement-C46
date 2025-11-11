package com.clinic.c46.StaffService.application.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateDepartmentRequest(
        @NotBlank(message = "Department name is required") @Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters") String name,

        @Size(max = 500, message = "Description must not exceed 500 characters") String description) {
}