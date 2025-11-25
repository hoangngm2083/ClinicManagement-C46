package com.clinic.c46.StaffService.application.listener;

import com.clinic.c46.CommonService.command.auth.CreateAccountCommand;
import com.clinic.c46.StaffService.domain.event.StaffCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffAccountCreationListener {

    private final CommandGateway commandGateway;

    @EventHandler
    public void on(StaffCreatedEvent event) {
        log.info("StaffCreatedEvent received for staffId: {}, dispatching CreateAccountCommand...", 
                event.staffId());

        try {
            String accountId = UUID.randomUUID().toString();
            String roleString = mapRoleToString(event.role().getCode());

            // Create command using common CreateAccountCommand class
            CreateAccountCommand command = CreateAccountCommand.builder()
                    .accountId(accountId)
                    .accountName(event.accountName())
                    .password(event.password())
                    .staffId(event.staffId())
                    .role(roleString)
                    .build();

            commandGateway.send(command)
                    .exceptionally(ex -> {
                        log.error("Failed to create account for staffId: {}", event.staffId(), ex);
                        return null;
                    })
                    .thenAccept(result -> {
                        log.info("Account creation command sent successfully for staffId: {}, accountName: {}", 
                                event.staffId(), event.accountName());
                    });

        } catch (Exception e) {
            log.error("Error dispatching CreateAccountCommand for staffId: {}", event.staffId(), e);
        }
    }

    private String mapRoleToString(int roleCode) {
        return switch (roleCode) {
            case 0 -> "DOCTOR";
            case 1 -> "RECEPTIONIST";
            case 2 -> "MANAGER";
            case 3 -> "ADMIN";
            default -> "USER";
        };
    }
}
