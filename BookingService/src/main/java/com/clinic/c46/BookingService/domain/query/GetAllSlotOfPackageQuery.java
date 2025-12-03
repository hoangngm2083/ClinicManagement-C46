package com.clinic.c46.BookingService.domain.query;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record GetAllSlotOfPackageQuery(String medicalPackageId, LocalDate dateFrom, LocalDate dateTo) {
}
