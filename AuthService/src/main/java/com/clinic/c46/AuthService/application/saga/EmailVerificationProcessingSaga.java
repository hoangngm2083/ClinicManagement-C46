package com.clinic.c46.AuthService.application.saga;

import com.clinic.c46.AuthService.application.service.EmailVerificationService;
import com.clinic.c46.AuthService.domain.event.EmailVerificationStartedEvent;
import com.clinic.c46.AuthService.domain.event.EmailVerificationPatientRepliedEvent;
import com.clinic.c46.CommonService.command.notification.SendOTPVerificationCommand;
import com.clinic.c46.CommonService.event.auth.EmailVerificationFailedEvent;
import com.clinic.c46.CommonService.event.auth.EmailVerifiedEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

@Saga
@Slf4j
@NoArgsConstructor
@ProcessingGroup("email-verification-saga")
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailVerificationProcessingSaga {
    private static final String DEADLINE_NAME = "email-verification-deadline";

    private transient final Duration EMAIL_VERIFICATION_TIMEOUT = Duration.ofSeconds(10000);
    @Autowired
    @JsonIgnore

    private transient CommandGateway commandGateway;
    @Autowired
    @JsonIgnore

    private transient DeadlineManager deadlineManager;
    @Autowired
    @JsonIgnore
    private transient EventGateway eventGateway;
    @Autowired
    @JsonIgnore
    private transient EmailVerificationService phoneVerificationService;

    private String verificationId;
    private String email;
    private String verificationCode;
    private String deadlineId;
    private EmailVerificationSagaStateMachine stateMachine;

    // === START SAGA ===
    @StartSaga
    @SagaEventHandler(associationProperty = "verificationId")
    public void handle(EmailVerificationStartedEvent event) {
        this.verificationId = event.verificationId();
        this.verificationCode = event.code();
        this.email = event.email();

        log.info("Starting email verification saga for: {}", email);

        this.deadlineId = deadlineManager.schedule(EMAIL_VERIFICATION_TIMEOUT, DEADLINE_NAME, this.verificationId);

        String callbackUrl = phoneVerificationService.buildCallbackUrl(this.verificationId, this.verificationCode);

        log.info("callbackUrl: {}", callbackUrl);

        commandGateway.send(SendOTPVerificationCommand.builder()
                .verificationId(this.verificationId)
                .callbackUrl(callbackUrl)
                .to(this.email)
                .verificationCode(this.verificationCode)
                .build());

        log.info("Sent ZNS notification command for verification: {}", verificationId);

        this.stateMachine = EmailVerificationSagaStateMachine.PENDING_PATIENT_REPLY;
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "verificationId")
    public void handle(EmailVerificationPatientRepliedEvent event) {
        if (!phoneVerificationService.validateOTP(event.verificationCode(), this.verificationCode)) {
            this.stateMachine = EmailVerificationSagaStateMachine.VALIDATE_OTP_FAILED;
            log.info("Invalid verification code for patient replied: {}", event.verificationCode());
            handleFailedVerification();
            return;
        }

        log.info("Verification [{}] completed!", event.verificationCode());
        destroyDeadline();
        eventGateway.publish(EmailVerifiedEvent.builder()
                .verificationId(this.verificationId)
                .email(this.email)
                .build());
    }

    @EndSaga
    @DeadlineHandler(deadlineName = DEADLINE_NAME)
    public void handle(String verificationId) {
        log.warn("Email verification timeout for: {}", verificationId);
        this.stateMachine = EmailVerificationSagaStateMachine.TIMEOUT;
        handleFailedVerification();
    }


    private void destroyDeadline() {
        deadlineManager.cancelSchedule(DEADLINE_NAME, deadlineId);
    }

    private void handleFailedVerification() {
        log.warn("====== handleFailedVerification: {} ======", verificationId);
        eventGateway.publish(EmailVerificationFailedEvent.builder()
                .verificationId(this.verificationId)
                .email(this.email)
                .reason(this.stateMachine.name())
                .build());


    }


}
