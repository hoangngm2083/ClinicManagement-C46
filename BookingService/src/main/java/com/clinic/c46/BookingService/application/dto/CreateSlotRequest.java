package com.clinic.c46.BookingService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CreateSlotRequest {
    private int shift;
    private LocalDate date;
    private String medicalPackageId;
    private int maxQuantity;
}
