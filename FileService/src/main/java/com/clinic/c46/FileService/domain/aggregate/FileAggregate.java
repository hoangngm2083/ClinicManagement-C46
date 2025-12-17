package com.clinic.c46.FileService.domain.aggregate;

import com.clinic.c46.CommonService.command.file.UploadCsvFileCommand;
import com.clinic.c46.CommonService.event.file.CsvFileUploadedEvent;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@Aggregate
@NoArgsConstructor
@Slf4j
public class FileAggregate {

    @AggregateIdentifier
    private String fileId;

    @Autowired
    private transient com.clinic.c46.FileService.infastructure.adapter.uploader.S3Uploader s3Uploader;

    @CommandHandler
    public FileAggregate(UploadCsvFileCommand cmd, 
                        com.clinic.c46.FileService.infastructure.adapter.uploader.S3Uploader s3Uploader) {
        if (cmd.fileId() == null || cmd.fileId().isBlank()) {
            throw new IllegalArgumentException("File ID không được để trống");
        }
        if (cmd.fileName() == null || cmd.fileName().isBlank()) {
            throw new IllegalArgumentException("File name không được để trống");
        }
        if (cmd.fileContent() == null || cmd.fileContent().length == 0) {
            throw new IllegalArgumentException("File content không được để trống");
        }

        log.info("Uploading CSV file: {} with ID: {}", cmd.fileName(), cmd.fileId());

        // Upload to S3
        String fileUrl = s3Uploader.uploadBytes(cmd.fileContent(), cmd.fileName(), cmd.contentType());

        log.info("CSV file uploaded successfully. URL: {}", fileUrl);

        // Emit event
        CsvFileUploadedEvent event = CsvFileUploadedEvent.builder()
                .fileId(cmd.fileId())
                .fileName(cmd.fileName())
                .fileUrl(fileUrl)
                .uploadedAt(LocalDateTime.now())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(CsvFileUploadedEvent event) {
        this.fileId = event.fileId();
    }
}
