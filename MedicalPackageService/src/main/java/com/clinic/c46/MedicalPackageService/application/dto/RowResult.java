package com.clinic.c46.MedicalPackageService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of processing a single row in bulk import.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RowResult {
    private int originalRowNum;
    private Map<String, String> data;
    private String status; // SUCCESS or FAILED
    private String message;
}
