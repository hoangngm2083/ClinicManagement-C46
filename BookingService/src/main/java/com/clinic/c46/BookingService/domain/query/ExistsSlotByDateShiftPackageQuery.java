package com.clinic.c46.BookingService.domain.query;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ExistsSlotByDateShiftPackageQuery(LocalDate date, int shift, String medicalPackageId) {
}
