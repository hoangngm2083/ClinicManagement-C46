package com.clinic.c46.MedicalPackageService.domain.command;


import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Set;

@Builder
public record UpdateMedicalPackageInfoCommand(@TargetAggregateIdentifier String medicalPackageId, String name, String description,
                                              Set<String> serviceIds, String image) {
}
