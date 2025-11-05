package com.clinic.c46.MedicalPackageService.domain.command;

import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

@Builder
public record UpdateMedicalPackagePriceCommand(@TargetAggregateIdentifier String medicalPackageId,
                                               BigDecimal newPrice) {
}
