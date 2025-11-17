package com.clinic.c46.BookingService.infrastructure.adapter.in.web.controller;

import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.command.CreateSlotCommand;
import com.clinic.c46.BookingService.domain.view.SlotView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slot/migration")
@RequiredArgsConstructor
@Slf4j
public class SlotMigrationController {

    private final SlotViewRepository slotViewRepository;
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
}
