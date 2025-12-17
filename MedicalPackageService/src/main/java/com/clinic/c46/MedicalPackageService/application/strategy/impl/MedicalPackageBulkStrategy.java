package com.clinic.c46.MedicalPackageService.application.strategy.impl;

import com.clinic.c46.MedicalPackageService.application.strategy.BulkOpsStrategy;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;
import com.clinic.c46.MedicalPackageService.domain.query.GetExistingMedicalServiceIdsQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MedicalPackageBulkStrategy implements BulkOpsStrategy {

    private static final String[] HEADERS = {"id", "name", "description", "serviceIds", "price", "image"};

    @Override
    public String[] getCsvHeaders() {
        return HEADERS;
    }

    @Override
    public void validateRow(Map<String, String> rowData, int rowNum, List<String> errors) {
        String id = rowData.get("id");
        String price = rowData.get("price");

        // If id exists and only price is provided -> update price scenario
        boolean isUpdatePriceOnly = id != null && !id.isBlank() &&
                price != null && !price.isBlank() &&
                (rowData.get("name") == null || rowData.get("name").isBlank());

        if (isUpdatePriceOnly) {
            // Validate price only
            validatePrice(price, rowNum, errors);
        } else if (id == null || id.isBlank()) {
            // Create scenario - validate all required fields
            validateRequiredField(rowData, "name", rowNum, errors);
            validateRequiredField(rowData, "price", rowNum, errors);
            validatePrice(rowData.get("price"), rowNum, errors);
        } else {
            // Update scenario - validate provided fields
            if (price != null && !price.isBlank()) {
                validatePrice(price, rowNum, errors);
            }
        }
    }

    private void validateRequiredField(Map<String, String> rowData, String field, int rowNum, List<String> errors) {
        String value = rowData.get(field);
        if (value == null || value.isBlank()) {
            errors.add(String.format("Row %d: Field '%s' is required", rowNum, field));
        }
    }

    private void validatePrice(String price, int rowNum, List<String> errors) {
        if (price == null || price.isBlank()) {
            return;
        }
        try {
            new BigDecimal(price);
        } catch (NumberFormatException e) {
            errors.add(String.format("Row %d: Invalid price format '%s'", rowNum, price));
        }
    }

    @Override
    public Set<String> extractForeignKeys(List<Map<String, String>> rows) {
        return rows.stream()
                .map(row -> row.get("serviceIds"))
                .filter(serviceIds -> serviceIds != null && !serviceIds.isBlank())
                .flatMap(serviceIds -> Arrays.stream(serviceIds.split(",")))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Boolean> validateForeignKeys(Set<String> foreignKeys, QueryGateway queryGateway) {
        if (foreignKeys.isEmpty()) {
            return Map.of();
        }

        GetExistingMedicalServiceIdsQuery query = GetExistingMedicalServiceIdsQuery.builder()
                .serviceIds(foreignKeys)
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
        String name = rowData.get("name");
        String price = rowData.get("price");

        // Determine operation type
        boolean isUpdatePriceOnly = id != null && !id.isBlank() &&
                price != null && !price.isBlank() &&
                (name == null || name.isBlank());

        if (id == null || id.isBlank()) {
            // CREATE operation
            String newId = UUID.randomUUID().toString();
            log.info("Bulk import [{}] - Row {}: CREATE MedicalPackage with generated ID: {}", 
                    bulkId, rowNum, newId);

            CreateMedicalPackageCommand cmd = CreateMedicalPackageCommand.builder()
                    .medicalPackageId(newId)
                    .name(rowData.get("name"))
                    .description(rowData.get("description"))
                    .serviceIds(parseServiceIds(rowData.get("serviceIds")))
                    .price(new BigDecimal(rowData.get("price")))
                    .image(rowData.get("image"))
                    .build();

            commandGateway.sendAndWait(cmd);
            log.info("Bulk import [{}] - Row {}: MedicalPackage created successfully", bulkId, rowNum);

        } else if (isUpdatePriceOnly) {
            // UPDATE PRICE operation
            log.info("Bulk import [{}] - Row {}: UPDATE_PRICE MedicalPackage ID: {}", 
                    bulkId, rowNum, id);

            UpdateMedicalPackagePriceCommand cmd = UpdateMedicalPackagePriceCommand.builder()
                    .medicalPackageId(id)
                    .newPrice(new BigDecimal(price))
                    .build();

            commandGateway.sendAndWait(cmd);
            log.info("Bulk import [{}] - Row {}: MedicalPackage price updated successfully", bulkId, rowNum);

        } else {
            // UPDATE INFO operation
            log.info("Bulk import [{}] - Row {}: UPDATE_INFO MedicalPackage ID: {}", 
                    bulkId, rowNum, id);

            UpdateMedicalPackageInfoCommand cmd = UpdateMedicalPackageInfoCommand.builder()
                    .medicalPackageId(id)
                    .name(rowData.get("name"))
                    .description(rowData.get("description"))
                    .serviceIds(parseServiceIds(rowData.get("serviceIds")))
                    .image(rowData.get("image"))
                    .build();

            commandGateway.sendAndWait(cmd);
            log.info("Bulk import [{}] - Row {}: MedicalPackage info updated successfully", bulkId, rowNum);
        }
    }

    private Set<String> parseServiceIds(String serviceIds) {
        if (serviceIds == null || serviceIds.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(serviceIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public String getEntityType() {
        return "MEDICAL_PACKAGE";
    }
}
