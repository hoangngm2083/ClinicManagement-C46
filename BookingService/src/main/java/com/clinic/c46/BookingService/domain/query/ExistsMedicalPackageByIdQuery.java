package com.clinic.c46.BookingService.domain.query;

import lombok.Builder;

@Builder
public record ExistsMedicalPackageByIdQuery(String medicalPackageId) {
}
