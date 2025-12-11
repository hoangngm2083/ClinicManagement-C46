package com.clinic.c46.AuthService.domain.aggregate;

import com.clinic.c46.AuthService.domain.event.AccountCreatedEvent;
import com.clinic.c46.CommonService.command.auth.CreateAccountCommand;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
@Slf4j
public class AccountAggregate {

    @AggregateIdentifier
    private String accountId;
    private String accountName;
    private String staffId;
    private String role;

    @CommandHandler
    public AccountAggregate(CreateAccountCommand command) {
        if (command.getAccountName() == null || command.getAccountName().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }
        if (command.getPassword() == null || command.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        log.info("Creating account aggregate for accountId: {}, accountName: {}", 
                command.getAccountId(), command.getAccountName());

        // Store password before applying event (password should not be in events for security)
        com.clinic.c46.AuthService.application.listener.AccountProjection.storePassword(
                command.getAccountId(), command.getPassword());

        AggregateLifecycle.apply(
                AccountCreatedEvent.builder()
                        .accountId(command.getAccountId())
                        .accountName(command.getAccountName())
                        .staffId(command.getStaffId())
                        .role(command.getRole())
                        .build()
        );
    }

    @EventSourcingHandler
    public void on(AccountCreatedEvent event) {
        this.accountId = event.accountId();
        this.accountName = event.accountName();
        this.staffId = event.staffId();
        this.role = event.role();
        
        log.info("Account aggregate created: accountId={}, accountName={}", 
                event.accountId(), event.accountName());
    }
}
