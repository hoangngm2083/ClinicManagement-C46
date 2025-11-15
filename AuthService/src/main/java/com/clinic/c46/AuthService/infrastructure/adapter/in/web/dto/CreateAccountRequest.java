package com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {
    
    @NotBlank(message = "Account name is required")
    @Size(min = 3, max = 50, message = "Account name must be between 3 and 50 characters")
    private String accountName;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    private String staffId;
    
    private String role = "USER";
}
