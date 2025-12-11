package com.clinic.c46.BookingService.domain.query;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GetAllSlotOfPackageQuery(String medicalPackageId, LocalDate dateFrom, LocalDate dateTo) {
}
