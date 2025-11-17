package com.clinic.c46.AuthService.application.service;

import com.clinic.c46.AuthService.application.repository.AccountRepository;
import com.clinic.c46.AuthService.domain.entity.Account;
import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.AuthResponse;
import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.CreateAccountRequest;
import com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto.LoginRequest;
import com.clinic.c46.AuthService.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Override
    @Transactional
    public AuthResponse createAccount(CreateAccountRequest request) {
        // Check if account already exists
        if (accountRepository.existsByAccountName(request.getAccountName())) {
            throw new IllegalArgumentException("Account name already exists");
        }
        
        // Create new account
        Account account = Account.builder()
                .accountName(request.getAccountName())
                .password(passwordEncoder.encode(request.getPassword()))
                .staffId(request.getStaffId())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .build();
        
        Account savedAccount = accountRepository.save(account);
        
        log.info("Account created successfully: {}", savedAccount.getAccountName());
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                savedAccount.getAccountName(), 
                savedAccount.getAccountId(),
                savedAccount.getStaffId(),
                savedAccount.getRole()
        );
        String refreshToken = jwtService.generateRefreshToken(savedAccount.getAccountName());
        
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
    @Override
    public AuthResponse login(LoginRequest request) {
        // Find account
        Account account = accountRepository.findByAccountNameAndIsDeletedFalse(request.getAccountName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        log.info("User logged in successfully: {}", account.getAccountName());
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                account.getAccountName(),
                account.getAccountId(),
                account.getStaffId(),
                account.getRole()
        );
        String refreshToken = jwtService.generateRefreshToken(account.getAccountName());
        
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // Extract username from refresh token
        String accountName = jwtService.extractUsername(refreshToken);
        
        // Validate token
        if (!jwtService.validateToken(refreshToken, accountName)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        // Find account
        Account account = accountRepository.findByAccountNameAndIsDeletedFalse(accountName)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        log.info("Token refreshed for user: {}", accountName);
        
        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(
                account.getAccountName(),
                account.getAccountId(),
                account.getStaffId(),
                account.getRole()
        );
        
        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
