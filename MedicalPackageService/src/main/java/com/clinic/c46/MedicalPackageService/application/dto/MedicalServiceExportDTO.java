package com.clinic.c46.MedicalPackageService.application.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicalServiceExportDTO {
    @CsvBindByName(column = "id")
    private String id;
    @CsvBindByName(column = "name")
    private String name;
    @CsvBindByName(column = "description")
    private String description;
    @CsvBindByName(column = "departmentName")
    private String departmentName;
    @CsvBindByName(column = "departmentId")
    private String departmentId;
    @CsvBindByName(column = "processingPriority")
    private int processingPriority;
    @CsvBindByName(column = "formTemplate")
    private String formTemplate;
    @CsvBindByName(column = "createdAt")
    private LocalDateTime createdAt;
    @CsvBindByName(column = "updatedAt")
    private LocalDateTime updatedAt;
    @CsvBindByName(column = "deletedAt")
    private LocalDateTime deletedAt;
}
