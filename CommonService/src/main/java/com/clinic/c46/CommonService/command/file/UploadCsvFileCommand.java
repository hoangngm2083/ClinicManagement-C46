package com.clinic.c46.CommonService.command.file;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record UploadCsvFileCommand(
        @TargetAggregateIdentifier String fileId,
        String fileName,
        byte[] fileContent,
        String contentType
) {
}
