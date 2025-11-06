package com.clinic.c46.BookingService.application.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponse {
    private String slotId;
    private String medicalPackageId;
    private LocalDate date;
    private int shift;
    private int maxQuantity;
    private int remainingQuantity;
}




