package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;

import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.domain.view.SlotView;
import com.clinic.c46.CommonService.type.Shift;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/slot/migration")
@RequiredArgsConstructor
@Slf4j
public class SlotMigrationController {

    private final SlotViewRepository slotViewRepository;
    private final MedicalPackageViewRepository medicalPackageViewRepository;
    private final CommandGateway commandGateway;

    /**
     * Migrate existing database slots to Axon event store
     * This creates SlotAggregates for slots that were inserted directly into DB
     */
    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrateExistingSlots() {
        List<SlotView> existingSlots = slotViewRepository.findAll();
        List<String> migratedSlots = new ArrayList<>();
        List<String> failedSlots = new ArrayList<>();

        log.info("Starting migration of {} slots", existingSlots.size());

        for (SlotView slot : existingSlots) {
            try {
                // Create command from existing slot data
                CreateSlotCommand command = CreateSlotCommand.builder()
                        .slotId(slot.getSlotId())
                        .medicalPackageId(slot.getMedicalPackageId())
                        .shift(slot.getShift())
                        .maxQuantity(slot.getMaxQuantity())
                        .date(slot.getDate())
                        .build();

                // Send to Axon - this will create the aggregate
                commandGateway.sendAndWait(command);
                migratedSlots.add(slot.getSlotId());
                log.info("Migrated slot: {}", slot.getSlotId());

            } catch (Exception e) {
                log.error("Failed to migrate slot: {}", slot.getSlotId(), e);
                failedSlots.add(slot.getSlotId());
            }
        }

        return ResponseEntity.ok(Map.of(
                "total", existingSlots.size(),
                "migrated", migratedSlots.size(),
                "failed", failedSlots.size(),
                "migratedSlots", migratedSlots,
                "failedSlots", failedSlots));
    }

    /**
     * Migrate a single slot by ID
     */
    @PostMapping("/migrate/{slotId}")
    public ResponseEntity<Map<String, String>> migrateSingleSlot(@PathVariable String slotId) {
        SlotView slot = slotViewRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        try {
            CreateSlotCommand command = CreateSlotCommand.builder()
                    .slotId(slot.getSlotId())
                    .medicalPackageId(slot.getMedicalPackageId())
                    .shift(slot.getShift())
                    .maxQuantity(slot.getMaxQuantity())
                    .date(slot.getDate())
                    .build();

            commandGateway.sendAndWait(command);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "slotId", slotId,
                    "message", "Slot migrated to Axon event store"));

        } catch (Exception e) {
            log.error("Failed to migrate slot: {}", slotId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "slotId", slotId,
                    "message", e.getMessage()));
        }
    }

    /**
     * Create new slots for all medical packages in database
     * This initializes slots for each medical package with predefined schedules
     */
    @PostMapping("/create-new-slots")
    public ResponseEntity<Map<String, Object>> createNewSlots() {
        try {
            log.info("Starting new slots creation for all medical packages");

            // Step 1: Get all medical packages from database
            List<MedicalPackageView> packages = medicalPackageViewRepository.findAll();
            log.info("Retrieved {} medical packages", packages.size());

            if (packages.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "No medical packages found in database"));
            }

            // Step 2: Create slots for each package
            List<String> createdSlots = new ArrayList<>();
            List<String> failedSlots = new ArrayList<>();

            for (MedicalPackageView pkg : packages) {
                try {
                    createSlotsForPackage(pkg.getMedicalPackageId(), createdSlots, failedSlots);
                } catch (Exception e) {
                    log.error("Failed to create slots for package: {}", pkg.getMedicalPackageId(), e);
                    failedSlots.add(pkg.getMedicalPackageId());
                }
            }

            log.info("New slots creation completed: {} created, {} failed", createdSlots.size(), failedSlots.size());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "totalPackages", packages.size(),
                    "slotsCreated", createdSlots.size(),
                    "slotsFailed", failedSlots.size(),
                    "createdSlots", createdSlots,
                    "failedSlots", failedSlots));

        } catch (Exception e) {
            log.error("Slots creation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Create slots for specific medical package ID
     */
    @PostMapping("/create-slots/{medicalPackageId}")
    public ResponseEntity<Map<String, Object>> createSlotsForPackageEndpoint(
            @PathVariable String medicalPackageId) {
        try {
            log.info("Creating slots for medical package: {}", medicalPackageId);

            // Verify package exists
            medicalPackageViewRepository.findById(medicalPackageId)
                    .orElseThrow(() -> new IllegalArgumentException("Medical package not found: " + medicalPackageId));

            List<String> createdSlots = new ArrayList<>();
            List<String> failedSlots = new ArrayList<>();

            createSlotsForPackage(medicalPackageId, createdSlots, failedSlots);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "medicalPackageId", medicalPackageId,
                    "slotsCreated", createdSlots.size(),
                    "slotsFailed", failedSlots.size(),
                    "createdSlots", createdSlots,
                    "failedSlots", failedSlots));

        } catch (Exception e) {
            log.error("Failed to create slots for package: {}", medicalPackageId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Create slots for a specific package with predefined schedule
     */
    private void createSlotsForPackage(String medicalPackageId, List<String> createdSlots, List<String> failedSlots) {
        log.info("Creating slots for package: {}", medicalPackageId);

        // Get predefined slot schedule
        List<SlotSchedule> schedules = getPredefinedSchedules();

        for (SlotSchedule schedule : schedules) {
            try {
                String slotId = UUID.randomUUID().toString();

                CreateSlotCommand command = CreateSlotCommand.builder()
                        .slotId(slotId)
                        .medicalPackageId(medicalPackageId)
                        .date(schedule.date)
                        .shift(schedule.shift)
                        .maxQuantity(schedule.maxQuantity)
                        .build();

                commandGateway.sendAndWait(command);
                createdSlots.add(slotId);
                log.debug("Created slot: {} for package: {} on date: {} shift: {}",
                        slotId, medicalPackageId, schedule.date, schedule.shift);

            } catch (Exception e) {
                log.error("Failed to create slot for package: {} with schedule: {}",
                        medicalPackageId, schedule, e);
                failedSlots.add(medicalPackageId + "-" + schedule.date + "-" + schedule.shift);
            }
        }
    }

    /**
     * Get predefined slot schedules for new packages
     * Creates slots for next 30 days with 2 shifts per day
     */
    private List<SlotSchedule> getPredefinedSchedules() {
        List<SlotSchedule> schedules = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Create slots for next 30 days
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);

            // Morning shift
            schedules.add(new SlotSchedule(date, Shift.MORNING.getCode(), 10));

            // Afternoon shift
            schedules.add(new SlotSchedule(date, Shift.AFTERNOON.getCode(), 10));
        }

        return schedules;
    }

    /**
     * Inner class to hold slot schedule information
     */
    private static class SlotSchedule {
        LocalDate date;
        int shift;
        int maxQuantity;

        SlotSchedule(LocalDate date, int shift, int maxQuantity) {
            this.date = date;
            this.shift = shift;
            this.maxQuantity = maxQuantity;
        }

        @Override
        public String toString() {
            return "SlotSchedule{" +
                    "date=" + date +
                    ", shift=" + shift +
                    ", maxQuantity=" + maxQuantity +
                    '}';
        }
    }
}

