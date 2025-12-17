package com.clinic.c46.MedicalPackageService.application.strategy.impl;

import com.clinic.c46.CommonService.query.department.GetExistingDepartmentIdsQuery;
import com.clinic.c46.MedicalPackageService.application.strategy.BulkOpsStrategy;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MedicalServiceBulkStrategy implements BulkOpsStrategy {

    private static final String[] HEADERS = {"id", "name", "processingPriority", "description", "departmentId", "formTemplate"};
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String[] getCsvHeaders() {
        return HEADERS;
    }

    @Override
    public void validateRow(Map<String, String> rowData, int rowNum, List<String> errors) {
        String id = rowData.get("id");

        if (id == null || id.isBlank()) {
            // Create scenario - validate required fields
            validateRequiredField(rowData, "name", rowNum, errors);
            validateRequiredField(rowData, "description", rowNum, errors);
            validateRequiredField(rowData, "departmentId", rowNum, errors);
        }

        // Validate processingPriority if provided
        String priority = rowData.get("processingPriority");
        if (priority != null && !priority.isBlank()) {
            validateProcessingPriority(priority, rowNum, errors);
        }

        // Validate formTemplate if provided
        String formTemplate = rowData.get("formTemplate");
        if (formTemplate != null && !formTemplate.isBlank()) {
            validateJsonFormat(formTemplate, rowNum, errors);
        }
    }

    private void validateRequiredField(Map<String, String> rowData, String field, int rowNum, List<String> errors) {
        String value = rowData.get(field);
        if (value == null || value.isBlank()) {
            errors.add(String.format("Row %d: Field '%s' is required", rowNum, field));
        }
    }

    private void validateProcessingPriority(String priority, int rowNum, List<String> errors) {
        try {
            Integer.parseInt(priority);
        } catch (NumberFormatException e) {
            errors.add(String.format("Row %d: Invalid processingPriority format '%s'", rowNum, priority));
        }
    }

    private void validateJsonFormat(String json, int rowNum, List<String> errors) {
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            errors.add(String.format("Row %d: Invalid JSON format in formTemplate: %s", rowNum, e.getMessage()));
        }
    }

    @Override
    public Set<String> extractForeignKeys(List<Map<String, String>> rows) {
        return rows.stream()
                .map(row -> row.get("departmentId"))
                .filter(departmentId -> departmentId != null && !departmentId.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Boolean> validateForeignKeys(Set<String> foreignKeys, QueryGateway queryGateway) {
        if (foreignKeys.isEmpty()) {
            return Map.of();
        }

        GetExistingDepartmentIdsQuery query = GetExistingDepartmentIdsQuery.builder()
                .departmentIds(foreignKeys)
                .build();

        List<String> existingIds = queryGateway.query(query, ResponseTypes.multipleInstancesOf(String.class)).join();

        return foreignKeys.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        existingIds::contains
                ));
    }

    @Override
    public void processRow(Map<String, String> rowData, int rowNum, String bulkId, CommandGateway commandGateway) {
        String id = rowData.get("id");

        if (id == null || id.isBlank()) {
            // CREATE operation
            String newId = UUID.randomUUID().toString();
            log.info("Bulk import [{}] - Row {}: CREATE MedicalService with generated ID: {}", 
                    bulkId, rowNum, newId);

            CreateMedicalServiceCommand cmd = CreateMedicalServiceCommand.builder()
                    .medicalServiceId(newId)
                    .name(rowData.get("name"))
                    .processingPriority(parseProcessingPriority(rowData.get("processingPriority")))
                    .description(rowData.get("description"))
                    .departmentId(rowData.get("departmentId"))
                    .formTemplate(parseFormTemplate(rowData.get("formTemplate")))
                    .build();

            commandGateway.sendAndWait(cmd);
            log.info("Bulk import [{}] - Row {}: MedicalService created successfully", bulkId, rowNum);

        } else {
            // UPDATE operation
            log.info("Bulk import [{}] - Row {}: UPDATE MedicalService ID: {}", 
                    bulkId, rowNum, id);

            UpdateMedicalServiceInfoCommand cmd = UpdateMedicalServiceInfoCommand.builder()
                    .medicalServiceId(id)
                    .name(rowData.get("name"))
                    .processingPriority(parseProcessingPriority(rowData.get("processingPriority")))
                    .description(rowData.get("description"))
                    .departmentId(rowData.get("departmentId"))
                    .formTemplate(parseFormTemplate(rowData.get("formTemplate")))
                    .build();

            commandGateway.sendAndWait(cmd);
            log.info("Bulk import [{}] - Row {}: MedicalService updated successfully", bulkId, rowNum);
        }
    }

    private int parseProcessingPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return 0;
        }
        return Integer.parseInt(priority);
    }

    private JsonNode parseFormTemplate(String formTemplate) {
        if (formTemplate == null || formTemplate.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(formTemplate);
        } catch (Exception e) {
            log.error("Failed to parse formTemplate JSON: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getEntityType() {
        return "MEDICAL_SERVICE";
    }
}
