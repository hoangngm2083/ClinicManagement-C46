package com.clinic.c46.ExaminationFlowService.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record PackageRepDto(String id, BigDecimal price, Set<ServiceRepDto> services) {
}
