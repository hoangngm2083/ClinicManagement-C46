package com.clinic.c46.BookingService.application.handler.query.dto;


import lombok.Builder;

import java.time.LocalDate;

@Builder
public record SlotDto(String slotId, String medicalPackageId, LocalDate date, int shift) {

}
