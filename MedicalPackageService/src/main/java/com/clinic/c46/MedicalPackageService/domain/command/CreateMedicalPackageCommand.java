package com.clinic.c46.MedicalPackageService.domain.command;


import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;
import java.util.Set;

@Builder
public record CreateMedicalPackageCommand(@TargetAggregateIdentifier String medicalPackageId, String name,
                                          String description, Set<String> serviceIds, BigDecimal price
) {
}
