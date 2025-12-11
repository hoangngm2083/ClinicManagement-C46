package com.clinic.c46.AuthService.infrastructure.adapter.in.web;

import com.clinic.c46.AuthService.application.service.AuthService;
import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs for account creation, login, and token refresh")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create new account (Admin only)", 
        description = "Register a new account with username and password. Only accessible by ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AuthResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AuthResponse response = authService.createAccount(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "Login", 
        description = "Login with account credentials and receive access and refresh tokens. Copy the 'token' from response to use in Authorize button above.",
        security = {}
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh token", 
        description = "Get a new access token using refresh token",
        security = {}
    )
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    @Operation(
        summary = "Get current account info", 
        description = "Get current authenticated user's account information from bearer token. Requires authentication.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AccountInfoResponse> getCurrentAccount(Authentication authentication) {
        String accountName = authentication.getName();
        AccountInfoResponse response = authService.getCurrentAccount(accountName);
        return ResponseEntity.ok(response);
    }
}
