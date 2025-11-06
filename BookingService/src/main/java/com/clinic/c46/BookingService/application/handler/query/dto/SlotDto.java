package com.clinic.c46.BookingService.application.handler.query.dto;


import lombok.*;

import java.time.LocalDate;

//@Builder
//public record SlotDto(String slotId, String medicalPackageId, LocalDate date, int shift) {
//
//}

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotDto {
    private String slotId;
    private String medicalPackageId;
    private LocalDate date;
    private int shift;
}

