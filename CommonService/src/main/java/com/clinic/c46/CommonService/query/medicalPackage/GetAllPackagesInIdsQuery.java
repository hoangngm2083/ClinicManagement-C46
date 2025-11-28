package com.clinic.c46.CommonService.query.medicalPackage;

import lombok.Builder;

import java.util.Set;

@Builder
public record GetAllPackagesInIdsQuery(Set<String> ids) {
}

