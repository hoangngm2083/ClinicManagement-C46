package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

import java.util.Set;

@Builder
public record MedicalPackageInfoUpdatedEvent(String medicalPackageId, String name, String description,
                                             Set<String> serviceIds, int version, String image) {
}
