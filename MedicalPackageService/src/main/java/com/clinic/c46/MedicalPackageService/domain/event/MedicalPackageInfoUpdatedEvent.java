package com.clinic.c46.MedicalPackageService.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record MedicalPackageInfoUpdatedEvent(String medicalPackageId, String name, String description,
                                             Set<String> serviceIds, int version) {
}
