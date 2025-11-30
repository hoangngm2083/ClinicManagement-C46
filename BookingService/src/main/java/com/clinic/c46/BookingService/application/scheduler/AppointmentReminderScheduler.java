package com.clinic.c46.BookingService.application.scheduler;

import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.CommonService.command.notification.RemindAppointmentCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentViewRepository appointmentViewRepository;
    private final CommandGateway commandGateway;

    /**
     * Runs on a configurable schedule (default: every hour)
     * Finds appointments scheduled 24 hours from now and sends reminder emails
     */
    @Scheduled(cron = "${appointment.reminder.cron:0 0 * * * *}")
    public void sendAppointmentReminders() {
        log.info("Running appointment reminder scheduler");
        
        // Calculate the date 24 hours from now (tomorrow at this time)
        LocalDate reminderDate = LocalDate.now().plusDays(1);
        
        // Find all appointments in CREATED state for tomorrow
        List<AppointmentView> appointments = appointmentViewRepository.findByDateAndState(
                reminderDate, 
                AppointmentState.CREATED.name()
        );
        
        log.info("Found {} appointments scheduled for {} that need reminders", 
                appointments.size(), reminderDate);
        
        // Send reminder command for each appointment with proper async error handling
        appointments.forEach(appointment -> 
            sendReminderWithRetry(appointment.getId())
        );
    }

    /**
     * Sends reminder command with retry mechanism
     * Uses Spring Retry to automatically retry on failure
     */
    @org.springframework.retry.annotation.Retryable(
            maxAttempts = 3,
            backoff = @org.springframework.retry.annotation.Backoff(delay = 2000, multiplier = 2),
            retryFor = {Exception.class}
    )
    public void sendReminderWithRetry(String appointmentId) {
        RemindAppointmentCommand command = RemindAppointmentCommand.builder()
                .notificationId(UUID.randomUUID().toString())
                .appointmentId(appointmentId)
                .build();
        
        commandGateway.send(command)
                .thenAccept(result -> 
                    log.info("Successfully sent reminder command for appointment: {}", appointmentId)
                )
                .exceptionally(ex -> {
                    log.error("Failed to send reminder for appointment: {} after retries", appointmentId, ex);
                    // Rethrow to trigger Spring Retry
                    throw new RuntimeException("Failed to send reminder command", ex);
                })
                .join(); // Wait for completion to ensure retry works properly
    }

    /**
     * Recovery method called when all retry attempts are exhausted
     */
    @org.springframework.retry.annotation.Recover
    public void recoverFromSendFailure(Exception e, String appointmentId) {
        log.error("All retry attempts exhausted for appointment: {}. Manual intervention required.", 
                appointmentId, e);
        // TODO: Consider storing failed appointments in a separate table for manual retry
    }
}
