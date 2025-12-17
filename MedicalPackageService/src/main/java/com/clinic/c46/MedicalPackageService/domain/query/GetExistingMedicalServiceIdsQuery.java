package com.clinic.c46.MedicalPackageService.domain.query;

import lombok.Builder;

import java.util.Set;

@Builder
public record GetExistingMedicalServiceIdsQuery(Set<String> serviceIds) {
}
