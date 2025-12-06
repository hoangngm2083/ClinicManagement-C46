package com.clinic.c46.AuthService.application.listener;

import com.clinic.c46.AuthService.application.repository.AccountRepository;
import com.clinic.c46.AuthService.domain.entity.Account;
import com.clinic.c46.AuthService.domain.event.AccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountProjection {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Temporary storage for passwords during account creation flow
    private static final Map<String, String> passwordCache = new ConcurrentHashMap<>();
    
    public static void storePassword(String accountId, String password) {
        passwordCache.put(accountId, password);
    }

    @EventHandler
    public void on(AccountCreatedEvent event) {
        log.info("Creating account projection for accountId: {}, accountName: {}", 
                event.accountId(), event.accountName());

        try {
            String password = passwordCache.remove(event.accountId());
            if (password == null) {
                log.error("Password not found in cache for accountId: {}", event.accountId());
                throw new IllegalStateException("Password not found for account creation");
            }

            if (accountRepository.existsByAccountName(event.accountName())) {
                log.warn("Account already exists: {}", event.accountName());
                return;
            }

            Account account = Account.builder()
                    .accountName(event.accountName())
                    .password(passwordEncoder.encode(password))
                    .staffId(event.staffId())
                    .role(event.role() != null ? event.role() : "USER")
                    .isDeleted(false)
                    .build();

            accountRepository.save(account);
            
            log.info("Account projection saved successfully: accountName={}", event.accountName());
        } catch (Exception e) {
            log.error("Error creating account projection for accountId: {}", event.accountId(), e);
            passwordCache.remove(event.accountId()); // Clean up on error
            throw e;
        }
    }
}
