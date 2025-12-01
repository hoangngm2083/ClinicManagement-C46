package com.clinic.c46.BookingService.application.scheduler;

import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateSlotScheduler {

    private final SlotViewRepository slotViewRepository;
    private final MedicalPackageViewRepository medicalPackageViewRepository;
    private final CommandGateway commandGateway;

    @Value("${slot.creation.weeks-ahead:4}")
    private int weeksAhead;

    @Value("${slot.creation.default-max-quantity:50}")
    private int defaultMaxQuantity;

    /**
     * Runs once when application starts
     * Creates slots for the next X weeks
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - initializing slots for the next {} weeks", weeksAhead);
        createSlotsForInitialPeriod();
    }

    /**
     * Runs daily at midnight (configurable via cron expression)
     * Creates slots for the date that is X weeks from now
     */
    @Scheduled(cron = "${slot.creation.cron:0 0 0 * * *}")
    public void createDailySlots() {
        log.info("Running daily slot creation scheduler");
        
        // Calculate the date X weeks from now
        LocalDate targetDate = LocalDate.now().plusWeeks(weeksAhead);
        
        log.info("Creating slots for date: {}", targetDate);
        createSlotsForDate(targetDate);
    }

    /**
     * Creates slots for the initial period (X weeks from now)
     * Called once when application starts
     */
    @Transactional
    public void createSlotsForInitialPeriod() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(weeksAhead);
        
        log.info("Creating initial slots from {} to {}", startDate, endDate);
        
        int totalSlotsCreated = 0;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            int slotsCreated = createSlotsForDate(date);
            totalSlotsCreated += slotsCreated;
        }
        
        log.info("Initial slot creation completed. Total slots created: {}", totalSlotsCreated);
    }

    /**
     * Creates slots for a specific date
     * Creates 2 shifts (morning and afternoon) for each medical package
     * 
     * @param date The date to create slots for
     * @return Number of slots created
     */
    @Transactional
    public int createSlotsForDate(LocalDate date) {
        // Get all active medical packages
        List<MedicalPackageView> medicalPackages = medicalPackageViewRepository.findAll();
        
        if (medicalPackages.isEmpty()) {
            log.warn("No medical packages found. Skipping slot creation for date: {}", date);
            return 0;
        }
        
        int slotsCreated = 0;
        
        for (MedicalPackageView medicalPackage : medicalPackages) {
            // Create slots for both shifts (0 = morning, 1 = afternoon)
            for (int shift = 0; shift <= 1; shift++) {
                // Check if slot already exists
                if (slotViewRepository.existsByDateAndShiftAndMedicalPackageId(
                        date, shift, medicalPackage.getMedicalPackageId())) {
                    log.debug("Slot already exists for date: {}, shift: {}, package: {}. Skipping.",
                            date, shift, medicalPackage.getMedicalPackageName());
                    continue;
                }
                
                // Create slot with retry mechanism
                try {
                    createSlotWithRetry(date, shift, medicalPackage.getMedicalPackageId());
                    slotsCreated++;
                    log.debug("Created slot for date: {}, shift: {}, package: {}",
                            date, shift, medicalPackage.getMedicalPackageName());
                } catch (Exception e) {
                    log.error("Failed to create slot for date: {}, shift: {}, package: {}",
                            date, shift, medicalPackage.getMedicalPackageName(), e);
                }
            }
        }
        
        log.info("Created {} slots for date: {}", slotsCreated, date);
        return slotsCreated;
    }

    /**
     * Creates a single slot with retry mechanism
     * Uses Spring Retry to automatically retry on failure
     */
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class}
    )
    public void createSlotWithRetry(LocalDate date, int shift, String medicalPackageId) {
        String slotId = UUID.randomUUID().toString();
        
        CreateSlotCommand command = CreateSlotCommand.builder()
                .slotId(slotId)
                .date(date)
                .shift(shift)
                .medicalPackageId(medicalPackageId)
                .maxQuantity(defaultMaxQuantity)
                .build();
        
        commandGateway.send(command)
                .thenAccept(result -> 
                    log.trace("Successfully created slot: {}", slotId)
                )
                .exceptionally(ex -> {
                    log.error("Failed to create slot for date: {}, shift: {}, package: {}",
                            date, shift, medicalPackageId, ex);
                    throw new RuntimeException("Failed to create slot command", ex);
                })
                .join(); // Wait for completion to ensure retry works properly
    }

    /**
     * Recovery method called when all retry attempts are exhausted
     */
    @Recover
    public void recoverFromSlotCreationFailure(Exception e, LocalDate date, int shift, String medicalPackageId) {
        log.error("All retry attempts exhausted for slot creation. Date: {}, Shift: {}, Package: {}. " +
                "Manual intervention required.", date, shift, medicalPackageId, e);
        // TODO: Consider storing failed slot creations in a separate table for manual retry
    }
}
