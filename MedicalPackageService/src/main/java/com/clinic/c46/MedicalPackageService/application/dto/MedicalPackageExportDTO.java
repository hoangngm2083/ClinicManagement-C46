package com.clinic.c46.MedicalPackageService.application.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MedicalPackageExportDTO {
    @CsvBindByName(column = "id")
    private String id;
    @CsvBindByName(column = "name")
    private String name;
    @CsvBindByName(column = "description")
    private String description;
    @CsvBindByName(column = "price")
    private BigDecimal price;
    @CsvBindByName(column = "serviceIds")
    private String serviceIds;
    @CsvBindByName(column = "serviceNames")
    private String serviceNames;
    @CsvBindByName(column = "createdAt")
    private LocalDateTime createdAt;
    @CsvBindByName(column = "updatedAt")
    private LocalDateTime updatedAt;
    @CsvBindByName(column = "deletedAt")
    private LocalDateTime deletedAt;
}
