package com.clinic.c46.MedicalPackageService.application.service;

public interface BulkImportService {
    /**
     * Starts a bulk import operation asynchronously.
     * Returns bulkId immediately for client polling.
     *
     * @param entityType Type of entity (MEDICAL_PACKAGE or MEDICAL_SERVICE)
     * @param csvUrl     URL of the CSV file to import
     * @return bulkId for tracking the import status
     */
    String startBulkImport(String entityType, String csvUrl);
}
