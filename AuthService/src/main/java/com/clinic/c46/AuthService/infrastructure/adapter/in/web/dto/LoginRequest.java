package com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "Account name is required")
    private String accountName;
    
    @NotBlank(message = "Password is required")
    private String password;
}
