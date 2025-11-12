package com.clinic.c46.StaffService.application.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Range;


public record CreateStaffRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") String name,

        @NotBlank(message = "Email is required") @Email(message = "Email should be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,

        @Pattern(regexp = "^(|\\+?[0-9\\s\\-()]{10,15})$", message = "Invalid phone number format") @Size(max = 15, message = "Phone number must not exceed 15 characters") String phone,

        @Size(max = 500, message = "Description must not exceed 500 characters") String description,

        @Size(max = 255, message = "Image path must not exceed 255 characters") String image,

        @NotNull(message = "Role is required") @Range(min = 0, max = 2, message = "Role must be 0, 1, or 2") int role,

        @Size(max = 255, message = "E-signature must not exceed 255 characters") String eSignature,

        @Pattern(regexp = "^(|[a-fA-F0-9\\-]{36})$", message = "Invalid department ID format") String departmentId) {
}