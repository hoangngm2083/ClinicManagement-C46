package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.MedicalPackageService.application.repository.BulkImportStatusRepository;
import com.clinic.c46.MedicalPackageService.domain.event.BulkImportCreatedEvent;
import com.clinic.c46.MedicalPackageService.domain.event.BulkImportStatusUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.view.BulkImportStatusView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BulkImportStatusProjection {

    private final BulkImportStatusRepository repository;

    @EventHandler
    public void on(BulkImportCreatedEvent event) {
        log.info("Creating bulk import status for bulkId: {}, entityType: {}", 
                event.bulkId(), event.entityType());

        BulkImportStatusView view = BulkImportStatusView.builder()
                .bulkId(event.bulkId())
                .entityType(event.entityType())
                .importFileUrl(event.importFileUrl())
                .status(event.status())
                .totalRows(0)
                .successfulRows(0)
                .failedRows(0)
                .build();

        view.markCreated();
        repository.save(view);

        log.info("Bulk import status created successfully for bulkId: {}", event.bulkId());
    }

    @EventHandler
    public void on(BulkImportStatusUpdatedEvent event) {
        log.info("Updating bulk import status for bulkId: {}, status: {}", 
                event.bulkId(), event.status());

        BulkImportStatusView view = repository.findById(event.bulkId())
                .orElseThrow(() -> new IllegalStateException(
                        "Bulk import status not found for bulkId: " + event.bulkId()));

        view.setStatus(event.status());
        view.setTotalRows(event.totalRows());
        view.setSuccessfulRows(event.successfulRows());
        view.setFailedRows(event.failedRows());
        view.setResultCsvUrl(event.resultCsvUrl());
        view.markUpdated();

        repository.save(view);

        log.info("Bulk import status updated successfully for bulkId: {}", event.bulkId());
    }
}
