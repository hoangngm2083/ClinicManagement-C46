package com.clinic.c46.MedicalPackageService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record UpdateBulkImportStatusCommand(
        @TargetAggregateIdentifier String bulkId,
        String status,
        Integer totalRows,
        Integer successfulRows,
        Integer failedRows,
        String resultCsvUrl
) {
}
