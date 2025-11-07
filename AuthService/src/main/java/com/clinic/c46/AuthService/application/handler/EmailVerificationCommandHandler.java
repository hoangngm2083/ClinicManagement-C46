package com.clinic.c46.AuthService.application.handler;

import com.clinic.c46.AuthService.application.service.EmailVerificationServiceImpl;
import com.clinic.c46.AuthService.domain.event.EmailVerificationStartedEvent;
import com.clinic.c46.CommonService.command.auth.VerifyEmailCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationCommandHandler {
    private final CommandGateway commandGateway;

    private final EventGateway eventGateway;

    private final EmailVerificationServiceImpl emailVerificationService;

    @CommandHandler
    public void handle(VerifyEmailCommand command) {

        log.warn("===================== EmailVerificationCommandHandler ========================");

        eventGateway.publish(EmailVerificationStartedEvent.builder()
                .email(command.email())
                .verificationId(command.verificationId())
                .code(emailVerificationService.generateOTP())
                .build());

        log.warn("===================== eventGateway.publish(EmailVerificationStartedEvent) ========================");

    }
}
