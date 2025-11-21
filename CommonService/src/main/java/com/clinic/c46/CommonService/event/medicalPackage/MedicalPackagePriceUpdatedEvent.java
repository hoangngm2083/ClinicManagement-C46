package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MedicalPackagePriceUpdatedEvent(String medicalPackageId, int newVersion, BigDecimal newPrice) {
}
