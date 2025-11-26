package com.clinic.c46.PaymentService.infrastructure.adapter.controller;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.examinationFlow.GetAllMedicalFormsQuery;
import com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery;
import com.clinic.c46.CommonService.dto.MedicalFormDto;
import com.clinic.c46.CommonService.command.payment.CreateInvoiceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/migration/invoice")
@RequiredArgsConstructor
@Slf4j
public class InvoiceMigrationController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    /**
     * Migrate invoice for a specific medical form
     * Query để lấy medicalForm từ DB, sau đó tạo invoice
     */
    @PostMapping("/migrate/{medicalFormId}")
    public CompletableFuture<ResponseEntity<Map<String, String>>> migrateInvoiceByMedicalForm(
            @PathVariable String medicalFormId) {
        log.info("Starting invoice migration for medicalFormId: {}", medicalFormId);

        // Step 1: Query để lấy medicalForm từ QueryGateway
        return queryGateway.query(new GetMedicalFormByIdQuery(medicalFormId),
                        ResponseTypes.optionalInstanceOf(MedicalFormDto.class))
                .thenApply(result -> result.orElseThrow(
                        () -> new ResourceNotFoundException("Phiếu khám bệnh")))
                // Step 2: Create invoice command và gửi qua CommandGateway
                .thenCompose(medicalFormDto -> {
                    log.info("Medical form found. Creating invoice for patient: {}", medicalFormDto.patientId());

                    String invoiceId = UUID.randomUUID().toString();
                    
                    CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                            .invoiceId(invoiceId)
                            .medicalFormId(medicalFormId)
                            .build();

                    return commandGateway.send(command)
                            .thenApply(resp -> {
                                Map<String, String> response = new HashMap<>();
                                response.put("invoiceId", invoiceId);
                                response.put("medicalFormId", medicalFormId);
                                response.put("patientId", medicalFormDto.patientId());
                                response.put("status", "CREATED");
                                return ResponseEntity.status(HttpStatus.CREATED).body(response);
                            });
                });
    }

    /**
     * Migrate invoices for all medical forms in database
     * Step 1: Query QueryGateway để lấy tất cả medicalFormIds từ DB
     * Step 2: Migrate invoices dựa trên medicalFormIds nhận được
     */
    @PostMapping("/batch")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> migrateInvoicesBatch() {
        log.info("Starting batch invoice migration - querying all medical forms from database");

        // Step 1: Query tất cả medical forms từ DB qua QueryGateway
        return queryGateway.query(new GetAllMedicalFormsQuery(),
                ResponseTypes.multipleInstancesOf(MedicalFormDto.class))
                .thenCompose(medicalForms -> {
                    log.info("Found {} medical forms in database. Starting migration...", medicalForms.size());

                    if (medicalForms.isEmpty()) {
                        log.warn("No medical forms found in database");
                        Map<String, Object> response = new HashMap<>();
                        response.put("totalMedicalForms", 0);
                        response.put("totalMigrated", 0);
                        response.put("migrationResults", Collections.emptyList());
                        response.put("status", "NO_MEDICAL_FORMS");
                        return CompletableFuture.completedFuture(ResponseEntity.ok(response));
                    }

                    // Step 2: Migrate invoices cho từng medical form
                    List<CompletableFuture<Map<String, String>>> migrationFutures = medicalForms.stream()
                            .map(medicalForm -> {
                                String invoiceId = UUID.randomUUID().toString();
                                
                                CreateInvoiceCommand command = CreateInvoiceCommand.builder()
                                        .invoiceId(invoiceId)
                                        .medicalFormId(medicalForm.id())
                                        .build();

                                return commandGateway.send(command)
                                        .thenApply(resp -> {
                                            Map<String, String> result = new HashMap<>();
                                            result.put("invoiceId", invoiceId);
                                            result.put("medicalFormId", medicalForm.id());
                                            result.put("patientId", medicalForm.patientId());
                                            result.put("status", "CREATED");
                                            log.debug("Invoice created: invoiceId={}, medicalFormId={}", invoiceId, medicalForm.id());
                                            return result;
                                        });
                            })
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(migrationFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                List<Map<String, String>> results = migrationFutures.stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.toList());

                                Map<String, Object> response = new HashMap<>();
                                response.put("totalMedicalForms", medicalForms.size());
                                response.put("totalMigrated", results.size());
                                response.put("migrationResults", results);
                                response.put("status", "BATCH_MIGRATION_COMPLETED");

                                log.info("Batch migration completed. Total migrated: {}/{}", results.size(), medicalForms.size());
                                return ResponseEntity.status(HttpStatus.CREATED).body(response);
                            });
                })
                .exceptionally(ex -> {
                    log.error("Error during batch migration", ex);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", ex.getMessage());
                    errorResponse.put("status", "MIGRATION_FAILED");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }
}
