package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record MedicalPackageCreatedEvent(String medicalPackageId, String name, String description, BigDecimal price,
                                         Set<String> serviceIds, String image, int priceVersion) {

}
