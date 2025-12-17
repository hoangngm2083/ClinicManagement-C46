package com.clinic.c46.MedicalPackageService.application.template.impl;

import com.clinic.c46.MedicalPackageService.application.dto.RowResult;
import com.clinic.c46.MedicalPackageService.application.factory.BulkOpsStrategyFactory;
import com.clinic.c46.MedicalPackageService.application.strategy.BulkOpsStrategy;
import com.google.common.collect.Lists;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BulkImportTemplateImpl {

    private final BulkOpsStrategyFactory strategyFactory;
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Value("${bulk.import.batch-size}")
    private int batchSize;

    @Value("${bulk.import.max-concurrent}")
    private int maxConcurrent;

    /**
     * Template method for executing bulk import.
     * Returns list of RowResult for all rows (both successful and failed).
     */
    public List<RowResult> executeBulkImport(String bulkId, String entityType, String csvUrl) {
        log.info("Starting bulk import [{}] for entity type: {}", bulkId, entityType);

        BulkOpsStrategy strategy = strategyFactory.getStrategy(entityType);

        // Step 1: Parse CSV
        List<Map<String, String>> rows = parseCsv(csvUrl, strategy);
        log.info("Bulk import [{}]: Parsed {} rows from CSV", bulkId, rows.size());

        // Step 2: Validate data types
        List<RowResult> results = validateDataTypes(rows, strategy);
        log.info("Bulk import [{}]: Data type validation complete. {} rows with errors", 
                bulkId, results.stream().filter(r -> "FAILED".equals(r.getStatus())).count());

        // Step 3: Validate foreign keys
        validateForeignKeys(results, strategy);
        log.info("Bulk import [{}]: Foreign key validation complete", bulkId);

        // Step 4: Import valid rows asynchronously with parallel processing
        importCsvAsync(results, strategy, bulkId);
        log.info("Bulk import [{}]: Import processing complete", bulkId);

        return results;
    }

    /**
     * Step 1: Parse CSV file from URL.
     */
    private List<Map<String, String>> parseCsv(String csvUrl, BulkOpsStrategy strategy) {
        List<Map<String, String>> rows = new ArrayList<>();
        String[] expectedHeaders = strategy.getCsvHeaders();

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new URL(csvUrl).openStream()))
                .build()) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Validate headers
            if (!Arrays.equals(headers, expectedHeaders)) {
                throw new IllegalArgumentException(
                        String.format("Invalid CSV headers. Expected: %s, Got: %s",
                                Arrays.toString(expectedHeaders), Arrays.toString(headers)));
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> rowData = new HashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    rowData.put(headers[i], line[i]);
                }
                rows.add(rowData);
            }

        } catch (Exception e) {
            log.error("Failed to parse CSV from URL: {}", csvUrl, e);
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage(), e);
        }

        return rows;
    }

    /**
     * Step 2: Validate data types and constraints for each row.
     */
    private List<RowResult> validateDataTypes(List<Map<String, String>> rows, BulkOpsStrategy strategy) {
        List<RowResult> results = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // +2 because CSV is 1-indexed and has header row
            Map<String, String> rowData = rows.get(i);
            List<String> errors = new ArrayList<>();

            strategy.validateRow(rowData, rowNum, errors);

            RowResult result = RowResult.builder()
                    .originalRowNum(rowNum)
                    .data(rowData)
                    .status(errors.isEmpty() ? "PENDING" : "FAILED")
                    .message(errors.isEmpty() ? null : String.join("; ", errors))
                    .build();

            results.add(result);
        }

        return results;
    }

    /**
     * Step 3: Validate foreign keys in batch.
     */
    private void validateForeignKeys(List<RowResult> results, BulkOpsStrategy strategy) {
        // Extract foreign keys from valid rows only
        List<Map<String, String>> validRows = results.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .map(RowResult::getData)
                .collect(Collectors.toList());

        if (validRows.isEmpty()) {
            return;
        }

        Set<String> foreignKeys = strategy.extractForeignKeys(validRows);
        if (foreignKeys.isEmpty()) {
            return;
        }

        Map<String, Boolean> fkValidation = strategy.validateForeignKeys(foreignKeys, queryGateway);

        // Mark rows with invalid foreign keys as FAILED
        for (RowResult result : results) {
            if (!"PENDING".equals(result.getStatus())) {
                continue;
            }

            List<String> invalidFks = new ArrayList<>();
            Set<String> rowFks = strategy.extractForeignKeys(List.of(result.getData()));

            for (String fk : rowFks) {
                Boolean exists = fkValidation.get(fk);
                if (exists == null || !exists) {
                    invalidFks.add(fk);
                }
            }

            if (!invalidFks.isEmpty()) {
                result.setStatus("FAILED");
                result.setMessage("Invalid foreign keys: " + String.join(", ", invalidFks));
            }
        }
    }

    /**
     * Step 4: Import valid rows with parallel processing and rate limiting.
     */
    private void importCsvAsync(List<RowResult> results, BulkOpsStrategy strategy, String bulkId) {
        List<RowResult> validRows = results.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());

        if (validRows.isEmpty()) {
            log.info("Bulk import [{}]: No valid rows to process", bulkId);
            return;
        }

        log.info("Bulk import [{}]: Processing {} valid rows in batches of {}", 
                bulkId, validRows.size(), batchSize);

        // Process in batches with parallel streams
        List<List<RowResult>> batches = Lists.partition(validRows, batchSize);

        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            List<RowResult> batch = batches.get(batchIndex);
            log.info("Bulk import [{}]: Processing batch {}/{} with {} rows", 
                    bulkId, batchIndex + 1, batches.size(), batch.size());

            batch.parallelStream()
                    .limit(maxConcurrent)
                    .forEach(row -> processRow(row, strategy, bulkId));
        }
    }

    /**
     * Process a single row with error handling.
     */
    private void processRow(RowResult row, BulkOpsStrategy strategy, String bulkId) {
        try {
            strategy.processRow(row.getData(), row.getOriginalRowNum(), bulkId, commandGateway);
            row.setStatus("SUCCESS");
            row.setMessage("Processed successfully");
        } catch (Exception e) {
            log.error("Bulk import [{}] - Row {}: Failed to process row", 
                    bulkId, row.getOriginalRowNum(), e);
            row.setStatus("FAILED");
            row.setMessage("Processing error: " + e.getMessage());
        }
    }
}
