package com.clinic.c46.BookingService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private String id;
    private String patientId;
    private String patientName;
    private int shift;
    private LocalDate date;
    private String medicalPackageId;
    private String medicalPackageName;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
