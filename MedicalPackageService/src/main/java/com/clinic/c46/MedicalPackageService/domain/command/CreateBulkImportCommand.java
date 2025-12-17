package com.clinic.c46.MedicalPackageService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record CreateBulkImportCommand(
        @TargetAggregateIdentifier String bulkId,
        String entityType,
        String importFileUrl
) {
}
