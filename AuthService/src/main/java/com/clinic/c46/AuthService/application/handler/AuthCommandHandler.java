package com.clinic.c46.AuthService.application.handler;


import com.clinic.c46.CommonService.command.auth.VerifyPhoneCommand;
import com.clinic.c46.CommonService.event.auth.PhoneVerificationFailedEvent;
import com.clinic.c46.CommonService.event.auth.PhoneVerifiedEvent;
import lombok.AllArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthCommandHandler {
    private final EventGateway eventGateway;
//    private final VerificationService verificationService;

    @CommandHandler
    public void handle(VerifyPhoneCommand command) {

        try {
//             verificationService.generateOTP();
            eventGateway.publish(PhoneVerifiedEvent.builder()
                    .phone(command.phone())
                    .causalId(command.causalId())
                    .build());
        } catch (Exception e) {
            eventGateway.publish(PhoneVerificationFailedEvent.builder()
                    .causalId(command.causalId())
                    .phone(command.phone())
                    .reason(e.getMessage())
                    .build());
        }


    }
}
