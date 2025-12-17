package com.clinic.c46.MedicalPackageService.application.strategy;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy interface for bulk operations on different entity types.
 * Defines the contract for CSV parsing, validation, and processing.
 */
public interface BulkOpsStrategy {

    /**
     * Returns the expected CSV headers for this entity type.
     */
    String[] getCsvHeaders();

    /**
     * Validates a single row's data types and constraints.
     * Adds error messages to the errors list if validation fails.
     *
     * @param rowData Map of column name to value
     * @param rowNum  Original row number in CSV (for error reporting)
     * @param errors  List to collect error messages
     */
    void validateRow(Map<String, String> rowData, int rowNum, List<String> errors);

    /**
     * Extracts all foreign key values from the rows for batch validation.
     *
     * @param rows List of row data maps
     * @return Set of foreign key IDs to validate
     */
    Set<String> extractForeignKeys(List<Map<String, String>> rows);

    /**
     * Validates foreign keys in batch using query gateway.
     *
     * @param foreignKeys  Set of foreign key IDs to validate
     * @param queryGateway QueryGateway for sending queries
     * @return Map of foreign key ID to existence status (true if exists)
     */
    Map<String, Boolean> validateForeignKeys(Set<String> foreignKeys, QueryGateway queryGateway);

    /**
     * Processes a single row by dispatching appropriate command (create or update).
     * Must log: bulkId, rowNum, entityType, row identifier, operation type.
     *
     * @param rowData       Map of column name to value
     * @param rowNum        Original row number in CSV
     * @param bulkId        Bulk import ID for logging
     * @param commandGateway CommandGateway for sending commands
     */
    void processRow(Map<String, String> rowData, int rowNum, String bulkId, CommandGateway commandGateway);

    /**
     * Returns the entity type name (e.g., "MEDICAL_PACKAGE", "MEDICAL_SERVICE").
     */
    String getEntityType();
}
