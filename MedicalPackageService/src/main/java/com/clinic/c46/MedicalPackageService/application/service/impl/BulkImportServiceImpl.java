package com.clinic.c46.MedicalPackageService.application.service.impl;

import com.clinic.c46.CommonService.command.file.UploadCsvFileCommand;
import com.clinic.c46.CommonService.event.file.CsvFileUploadedEvent;
import com.clinic.c46.MedicalPackageService.application.dto.RowResult;
import com.clinic.c46.MedicalPackageService.application.service.BulkImportService;
import com.clinic.c46.MedicalPackageService.application.template.impl.BulkImportTemplateImpl;
import com.clinic.c46.MedicalPackageService.domain.command.CreateBulkImportCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateBulkImportStatusCommand;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportServiceImpl implements BulkImportService {

    private final CommandGateway commandGateway;
    private final BulkImportTemplateImpl template;

    // Store pending bulk imports waiting for CSV upload
    private final Map<String, PendingBulkImport> pendingUploads = new ConcurrentHashMap<>();

    @Override
    public String startBulkImport(String entityType, String csvUrl) {
        // Generate bulk ID
        String bulkId = UUID.randomUUID().toString();
        log.info("Starting bulk import with bulkId: {}, entityType: {}", bulkId, entityType);

        // Create bulk import status synchronously
        CreateBulkImportCommand createCmd = CreateBulkImportCommand.builder()
                .bulkId(bulkId)
                .entityType(entityType)
                .importFileUrl(csvUrl)
                .build();

        commandGateway.sendAndWait(createCmd);
        log.info("Bulk import command sent for bulkId: {}", bulkId);

        // Launch async processing
        CompletableFuture.runAsync(() -> {
            try {
                // Execute bulk import
                List<RowResult> results = template.executeBulkImport(bulkId, entityType, csvUrl);

                // Calculate statistics
                long totalRows = results.size();
                long successfulRows = results.stream()
                        .filter(r -> "SUCCESS".equals(r.getStatus()))
                        .count();
                long failedRows = totalRows - successfulRows;

                log.info("Bulk import [{}]: Completed. Total: {}, Success: {}, Failed: {}",
                        bulkId, totalRows, successfulRows, failedRows);

                // Generate result CSV for failed rows only
                if (failedRows > 0) {
                    List<RowResult> failedResults = results.stream()
                            .filter(r -> "FAILED".equals(r.getStatus()))
                            .collect(Collectors.toList());

                    String resultCsv = generateResultCsv(failedResults);

                    // Upload result CSV to FileService
                    String fileId = UUID.randomUUID().toString();
                    UploadCsvFileCommand uploadCmd = UploadCsvFileCommand.builder()
                            .fileId(fileId)
                            .fileName("bulk_import_errors_" + bulkId + ".csv")
                            .fileContent(resultCsv.getBytes())
                            .contentType("text/csv")
                            .build();

                    // Store pending upload info
                    pendingUploads.put(fileId, new PendingBulkImport(
                            bulkId, totalRows, successfulRows, failedRows, "COMPLETED"));

                    commandGateway.sendAndWait(uploadCmd);
                    log.info("Bulk import [{}]: Result CSV upload command sent", bulkId);

                } else {
                    // No failed rows, update status directly
                    updateBulkStatus(bulkId, "COMPLETED", totalRows, successfulRows, failedRows, null);
                }

            } catch (Exception e) {
                log.error("Bulk import [{}]: Failed with error", bulkId, e);
                updateBulkStatus(bulkId, "FAILED", 0L, 0L, 0L, null);
            }
        });

        // Return bulkId immediately
        return bulkId;
    }

    /**
     * Event handler to receive CSV upload completion event from FileService.
     */
    @EventHandler
    public void on(CsvFileUploadedEvent event) {
        PendingBulkImport pending = pendingUploads.remove(event.fileId());
        if (pending != null) {
            log.info("Received CSV upload event for bulkId: {}, fileUrl: {}", 
                    pending.bulkId, event.fileUrl());
            updateBulkStatus(pending.bulkId, pending.status, 
                    pending.totalRows, pending.successfulRows, pending.failedRows, event.fileUrl());
        }
    }

    /**
     * Generate CSV file containing only failed rows with error details.
     */
    private String generateResultCsv(List<RowResult> failedResults) {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // Write header
            String[] header = {"original_row_num", "status", "message", "row_data"};
            csvWriter.writeNext(header);

            // Write failed rows
            for (RowResult result : failedResults) {
                String rowData = result.getData().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("; "));

                String[] row = {
                        String.valueOf(result.getOriginalRowNum()),
                        result.getStatus(),
                        result.getMessage(),
                        rowData
                };
                csvWriter.writeNext(row);
            }

            return stringWriter.toString();

        } catch (Exception e) {
            log.error("Failed to generate result CSV", e);
            throw new RuntimeException("Failed to generate result CSV", e);
        }
    }

    /**
     * Update bulk import status.
     */
    private void updateBulkStatus(String bulkId, String status, Long totalRows, 
                                   Long successfulRows, Long failedRows, String resultCsvUrl) {
        UpdateBulkImportStatusCommand updateCmd = UpdateBulkImportStatusCommand.builder()
                .bulkId(bulkId)
                .status(status)
                .totalRows(totalRows != null ? totalRows.intValue() : 0)
                .successfulRows(successfulRows != null ? successfulRows.intValue() : 0)
                .failedRows(failedRows != null ? failedRows.intValue() : 0)
                .resultCsvUrl(resultCsvUrl)
                .build();

        commandGateway.sendAndWait(updateCmd);
        log.info("Bulk import status updated for bulkId: {}, status: {}", bulkId, status);
    }

    /**
     * Helper class to store pending bulk import info.
     */
    private record PendingBulkImport(
            String bulkId,
            Long totalRows,
            Long successfulRows,
            Long failedRows,
            String status
    ) {}
}
