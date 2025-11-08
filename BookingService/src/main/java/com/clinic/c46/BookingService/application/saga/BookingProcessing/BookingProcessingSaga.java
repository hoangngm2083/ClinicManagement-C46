package com.clinic.c46.BookingService.application.saga.BookingProcessing;


import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.ReleaseFingerprintCommand;
import com.clinic.c46.BookingService.domain.command.ReleaseLockedSlotCommand;
import com.clinic.c46.BookingService.domain.event.*;
import com.clinic.c46.CommonService.command.auth.VerifyEmailCommand;
import com.clinic.c46.CommonService.command.patient.CreatePatientCommand;
import com.clinic.c46.CommonService.command.patient.DeletePatientCommand;
import com.clinic.c46.CommonService.command.patient.PatientCreationFailedEvent;
import com.clinic.c46.CommonService.event.auth.EmailVerificationFailedEvent;
import com.clinic.c46.CommonService.event.auth.EmailVerifiedEvent;
import com.clinic.c46.CommonService.event.patient.PatientCreatedEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

@Saga
@Slf4j
@NoArgsConstructor
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingProcessingSaga {
    private static final String DEADLINE_NAME = "booking-deadline";
    private static final int MAX_RETRY = 3;
    private static final String RETRY_CREATE_PATIENT = "retry-create-patient";
    private static final String RETRY_CREATE_APPOINTMENT = "retry-create-appointment";
    private final Duration BOOKING_TIMEOUT = Duration.ofSeconds(30);
    private final long SCHEDULE_RETRY = 30L;
    @Autowired
    @JsonIgnore
    private transient CommandGateway commandGateway;
    @Autowired
    @JsonIgnore
    private transient EventGateway eventGateway;
    @Autowired
    @JsonIgnore
    private transient DeadlineManager deadlineManager;
    private int retryCountPatient = 0;
    private int retryCountAppointment = 0;

    private BookingProcessingStateMachine stateMachine;

    private String reason;
    private String deadlineId;
    private String fingerprint;
    private String slotId;
    private String bookingId;
    private String name;
    private String phone;
    private String email;
    private String patientId;
    private String appointmentId;
    private String verificationId;

    @StartSaga
    @SagaEventHandler(associationProperty = "bookingId")
    private void handle(SlotLockedEvent event) {
        log.info("SlotLockedEvent in Saga for bookingId Id : {}", event.bookingId());
        this.stateMachine = BookingProcessingStateMachine.LOCKED;
        this.bookingId = event.bookingId();
        this.name = event.name();
        this.phone = event.phone();
        this.email = event.email();
        this.fingerprint = event.fingerprint();
        this.slotId = event.slotId();

        this.deadlineId = deadlineManager.schedule(BOOKING_TIMEOUT, DEADLINE_NAME, this.bookingId);

        this.verificationId = UUID.randomUUID()
                .toString();

        SagaLifecycle.associateWith("verificationId", this.verificationId);
        this.commandGateway.send(VerifyEmailCommand.builder()
                .verificationId(this.verificationId)
                .email(this.email)
                .build());

        this.stateMachine = BookingProcessingStateMachine.PENDING_VERIFY_PATIENT_EMAIL;
    }

    @SagaEventHandler(associationProperty = "verificationId")
    private void handle(EmailVerificationFailedEvent event) {
        this.reason = EmailVerificationFailedEvent.class.getSimpleName()
                .replace("Event", "");
        handleErrorOrTimeout();
    }

    @SagaEventHandler(associationProperty = "verificationId")
    private void handle(EmailVerifiedEvent event) {

        this.patientId = UUID.randomUUID()
                .toString();
        SagaLifecycle.associateWith("patientId", this.patientId);
        commandGateway.send(CreatePatientCommand.builder()
                .patientId(this.patientId)
                .phone(this.phone)
                .email(this.email)
                .name(this.name)
                .build());
        this.stateMachine = BookingProcessingStateMachine.PENDING_CREATE_PATIENT;
    }

    @SagaEventHandler(associationProperty = "patientId")
    private void on(PatientCreationFailedEvent event) {
        this.reason = PatientCreationFailedEvent.class.getSimpleName()
                .replace("Event", "");
        handleErrorOrTimeout();
    }

    @SagaEventHandler(associationProperty = "patientId")
    private void on(PatientCreatedEvent event) {
        this.appointmentId = UUID.randomUUID()
                .toString();

        SagaLifecycle.associateWith("appointmentId", this.appointmentId);

        this.commandGateway.send(CreateAppointmentCommand.builder()
                .appointmentId(this.appointmentId)
                .patientId(this.patientId)
                .slotId(this.slotId)
                .build());

        this.stateMachine = BookingProcessingStateMachine.PENDING_CREATE_APPOINTMENT;
    }

    @SagaEventHandler(associationProperty = "appointmentId")
    private void on(AppointmentCreationFailedEvent event) {
        this.reason = AppointmentCreationFailedEvent.class.getSimpleName()
                .replace("Event", "");
        handleErrorOrTimeout();
    }

    //    @EndSaga
    @SagaEventHandler(associationProperty = "appointmentId")
    private void handle(AppointmentCreatedEvent event) {
        this.stateMachine = BookingProcessingStateMachine.PENDING_RELEASE_SLOT_LOCKED;
        this.commandGateway.send(ReleaseFingerprintCommand.builder()
                .slotId(this.slotId)
                .fingerprint(this.fingerprint)
                .build());

    }

    @EndSaga
    @SagaEventHandler(associationProperty = "fingerprint")
    private void on(FingerprintReleasedEvent event) {
        this.cancelDeadline();
        this.eventGateway.publish(BookingCompletedEvent.builder()
                .bookingId(this.bookingId)
                .appointmentId(this.appointmentId)
                .patientId(this.patientId)
                .build());
        this.stateMachine = BookingProcessingStateMachine.COMPLETED;
    }


    @DeadlineHandler(deadlineName = DEADLINE_NAME)
    public void onBookingDeadline(String bookingId) {
        this.reason = BookingProcessingStateMachine.TIMEOUT.name();
        handleErrorOrTimeout();

    }


    private void handleErrorOrTimeout() {
        this.cancelDeadline();
        if (this.stateMachine == BookingProcessingStateMachine.PENDING_VERIFY_PATIENT_EMAIL || this.stateMachine == BookingProcessingStateMachine.LOCKED) {
            this.rollbackWithCompensation();
            return;
        }

        if (this.stateMachine == BookingProcessingStateMachine.PENDING_CREATE_PATIENT) {
            this.retryCreatePatient();
            return;
        }

        if (this.stateMachine == BookingProcessingStateMachine.PENDING_CREATE_APPOINTMENT) {
            this.retryCreateAppointment();
        }

    }

    private void rollbackWithCompensation() {
        log.warn("[rollbackWithCompensation] for [bookingId]: {}", this.bookingId);
        // 1. Release slot
        this.commandGateway.send(ReleaseLockedSlotCommand.builder()
                .slotId(this.slotId)
                .fingerprint(this.fingerprint)
                .build());

        // 2. Nếu patient đã tạo → gửi DeletePatientCommand (hoặc MarkAsInvalid)
        if (this.patientId != null && this.stateMachine == BookingProcessingStateMachine.PENDING_CREATE_APPOINTMENT) {
            commandGateway.send(DeletePatientCommand.builder()
                    .patientId(this.patientId)
                    .build());
        }
        log.error("[Publish Booking Rejected] for [bookingId]: {}, [state]: {}", this.bookingId, this.stateMachine);
        // 3. Publish rejected + end saga
        eventGateway.publish(BookingRejectedEvent.builder()
                .bookingId(this.bookingId)
                .reason(this.reason)
                .build());
        SagaLifecycle.end();
    }

    private void retryCreatePatient() {
        if (this.retryCountPatient >= MAX_RETRY) {
            log.error("Max retry exceeded for patient creation. Rollback.");
            rollbackWithCompensation();
            return;
        }
        this.retryCountPatient++;

        // schedule retry after e.g., 30s * retryCount
        Duration backoff = Duration.ofSeconds(this.SCHEDULE_RETRY * this.retryCountPatient);
        deadlineManager.schedule(backoff, RETRY_CREATE_PATIENT, this.patientId);
    }

    @DeadlineHandler(deadlineName = RETRY_CREATE_PATIENT)
    public void onRetryCreatePatient(String patientId) {
        // re-send create patient command (idempotent)
        commandGateway.send(CreatePatientCommand.builder()
                .patientId(this.patientId)
                .email(this.email)
                .name(this.name)
                .phone(this.phone)
                .build());
    }


    private void retryCreateAppointment() {
        if (this.retryCountAppointment >= MAX_RETRY) {
            log.error("[Max retry exceeded] for [appointment creation] => [Rollback].");
            rollbackWithCompensation();
            return;
        }
        this.retryCountAppointment++;
        // schedule retry after e.g., 30s * retryCount
        Duration backoff = Duration.ofSeconds(this.SCHEDULE_RETRY * this.retryCountAppointment);
        deadlineManager.schedule(backoff, RETRY_CREATE_APPOINTMENT, this.appointmentId);

    }

    @DeadlineHandler(deadlineName = RETRY_CREATE_APPOINTMENT)
    public void onRetryCreateAppointment(String appointmentId) {
        this.commandGateway.send(CreateAppointmentCommand.builder()
                .patientId(this.patientId)
                .appointmentId(this.appointmentId)
                .slotId(this.slotId)
                .build());
    }

    private void cancelDeadline() {
        if (deadlineId != null) {
            deadlineManager.cancelSchedule(DEADLINE_NAME, deadlineId);
            log.info("Deadline cancelled for bookingId: {}", bookingId);
        }
    }
}
