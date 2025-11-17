package com.clinic.c46.ExaminationFlowService.application.query;

import lombok.Builder;

import java.util.Set;

@Builder
public record GetAllServicesOfPackagesQuery(Set<String> packageIds) {
}
