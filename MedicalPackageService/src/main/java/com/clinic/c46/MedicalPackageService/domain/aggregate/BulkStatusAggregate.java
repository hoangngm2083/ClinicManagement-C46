package com.clinic.c46.MedicalPackageService.domain.aggregate;

import com.clinic.c46.MedicalPackageService.domain.command.CreateBulkImportCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateBulkImportStatusCommand;
import com.clinic.c46.MedicalPackageService.domain.event.BulkImportCreatedEvent;
import com.clinic.c46.MedicalPackageService.domain.event.BulkImportStatusUpdatedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;

@Aggregate
@NoArgsConstructor
public class BulkStatusAggregate {

    @AggregateIdentifier
    private String bulkId;
    private String status;

    @CommandHandler
    public BulkStatusAggregate(CreateBulkImportCommand cmd) {
        if (cmd.bulkId() == null || cmd.bulkId().isBlank()) {
            throw new IllegalArgumentException("Bulk ID không được để trống");
        }
        if (cmd.entityType() == null || cmd.entityType().isBlank()) {
            throw new IllegalArgumentException("Entity type không được để trống");
        }
        if (cmd.importFileUrl() == null || cmd.importFileUrl().isBlank()) {
            throw new IllegalArgumentException("Import file URL không được để trống");
        }

        BulkImportCreatedEvent event = BulkImportCreatedEvent.builder()
                .bulkId(cmd.bulkId())
                .entityType(cmd.entityType())
                .importFileUrl(cmd.importFileUrl())
                .status("PROCESSING")
                .createdAt(LocalDateTime.now())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(BulkImportCreatedEvent event) {
        this.bulkId = event.bulkId();
        this.status = event.status();
    }

    @CommandHandler
    public void handle(UpdateBulkImportStatusCommand cmd) {
        if (cmd.status() == null || cmd.status().isBlank()) {
            throw new IllegalArgumentException("Status không được để trống");
        }

        BulkImportStatusUpdatedEvent event = BulkImportStatusUpdatedEvent.builder()
                .bulkId(cmd.bulkId())
                .status(cmd.status())
                .totalRows(cmd.totalRows())
                .successfulRows(cmd.successfulRows())
                .failedRows(cmd.failedRows())
                .resultCsvUrl(cmd.resultCsvUrl())
                .updatedAt(LocalDateTime.now())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(BulkImportStatusUpdatedEvent event) {
        this.status = event.status();
    }
}
