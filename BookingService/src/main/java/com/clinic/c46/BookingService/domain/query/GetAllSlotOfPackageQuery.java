package com.clinic.c46.BookingService.domain.query;

import lombok.Builder;

@Builder
public record GetAllSlotOfPackageQuery(String medicalPackageId) {
}
