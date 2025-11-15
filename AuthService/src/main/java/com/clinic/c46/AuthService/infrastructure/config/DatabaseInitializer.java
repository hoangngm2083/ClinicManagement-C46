package com.clinic.c46.AuthService.infrastructure.config;

import com.clinic.c46.AuthService.application.repository.AccountRepository;
import com.clinic.c46.AuthService.domain.entity.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {
    
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        // Check if admin account already exists
        if (accountRepository.existsByAccountName("admin")) {
            log.info("Admin account already exists. Skipping initialization.");
            return;
        }
        
        // Create admin account
        // Password: admin123 (encoded with configured BCrypt strength)
        Account adminAccount = Account.builder()
                .accountName("admin")
                .password(passwordEncoder.encode("admin123"))
                .role("ADMIN")
                .staffId(null)
                .build();
        
        accountRepository.save(adminAccount);
        log.info("✅ Admin account created successfully! Username: admin, Password: admin123");
        log.warn("⚠️  SECURITY WARNING: Please change the default admin password after first login!");
    }
}
