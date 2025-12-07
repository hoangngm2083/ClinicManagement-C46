package com.clinic.c46.CommonService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDetailsDto {
    private String id;
    private String patientId;
    private String patientName;
    private String patientEmail;
    private int shift;
    private LocalDate date;
    private String medicalPackageId;
    private String medicalPackageName;
    private BigDecimal snapshotPrice;
    private int snapshotPriceVersion;
    private String state;
    private Set<ServiceDto> services;
    
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceDto {
        private String id;
        private String name;
    }
}
