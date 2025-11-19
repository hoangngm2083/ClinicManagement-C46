package com.clinic.c46.ExaminationFlowService.application.query;

import java.util.Set;

public record GetAllPackageByIdsQuery(Set<String> packageIds) {
}
