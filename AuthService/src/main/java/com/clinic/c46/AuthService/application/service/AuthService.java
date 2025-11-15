package com.clinic.c46.AuthService.application.service;

import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.AuthResponse;
import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.CreateAccountRequest;
import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.LoginRequest;

public interface AuthService {
    AuthResponse createAccount(CreateAccountRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
}
