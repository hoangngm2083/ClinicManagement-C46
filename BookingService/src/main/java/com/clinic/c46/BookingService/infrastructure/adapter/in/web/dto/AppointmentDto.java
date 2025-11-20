package com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
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
