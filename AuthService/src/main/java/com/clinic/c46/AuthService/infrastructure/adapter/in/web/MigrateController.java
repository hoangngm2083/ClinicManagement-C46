package com.clinic.c46.AuthService.infrastructure.adapter.in.web;

import com.clinic.c46.AuthService.application.repository.AccountRepository;
import com.clinic.c46.AuthService.domain.entity.Account;
import com.clinic.c46.CommonService.query.staff.GetIdOfAllStaffQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/migrate")
@RequiredArgsConstructor
@Slf4j
public class MigrateController {

    private final QueryGateway queryGateway;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Migrate database: Create accounts for all staff IDs
     * Each staff member gets an account with default credentials
     */
    @PostMapping("/database")
    public ResponseEntity<Map<String, Object>> migrateDatabase() {
        try {
            log.info("Starting account migration...");

            // Step 1: Query all staff IDs from StaffService
            CompletableFuture<List<String>> staffIds = queryGateway.query(new GetIdOfAllStaffQuery(),
                    ResponseTypes.multipleInstancesOf(String.class));

            List<String> allStaffIds = staffIds.get();
            log.info("Retrieved {} staff members", allStaffIds.size());

            if (allStaffIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "No staff found in the system"));
            }

            // Step 2: Create accounts for each staff
            List<String> createdAccounts = new ArrayList<>();
            List<String> skippedAccounts = new ArrayList<>();
            List<String> failedAccounts = new ArrayList<>();

            for (String staffId : allStaffIds) {
                try {
                    // Check if account already exists for this staff
                    String accountName = "staff_" + staffId;
                    if (accountRepository.existsByAccountName(accountName)) {
                        skippedAccounts.add(staffId);
                        log.debug("Account already exists for staff: {}", staffId);
                        continue;
                    }

                    // Create new account
                    Account account = Account.builder()
                            .accountName(accountName)
                            .password(passwordEncoder.encode("password123")) // Default password
                            .staffId(staffId)
                            .role("USER")
                            .isDeleted(false)
                            .build();

                    accountRepository.save(account);
                    createdAccounts.add(staffId);
                    log.debug("Created account for staff: {}", staffId);

                } catch (Exception e) {
                    log.error("Failed to create account for staff: {}", staffId, e);
                    failedAccounts.add(staffId);
                }
            }

            log.info("Account migration completed: {} created, {} skipped, {} failed", createdAccounts.size(),
                    skippedAccounts.size(), failedAccounts.size());

            return ResponseEntity.ok(Map.of("status", "success", "totalStaff", allStaffIds.size(), "accountsCreated",
                    createdAccounts.size(), "accountsSkipped", skippedAccounts.size(), "accountsFailed",
                    failedAccounts.size(), "createdStaffIds", createdAccounts, "skippedStaffIds", skippedAccounts,
                    "failedStaffIds", failedAccounts));

        } catch (Exception e) {
            log.error("Account migration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
