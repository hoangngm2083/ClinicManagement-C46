package com.clinic.c46.CommonService.event.medicalPackage;

import lombok.Builder;

@Builder
public record MedicalPackageDeletedEvent(String medicalPackageId) {
}
