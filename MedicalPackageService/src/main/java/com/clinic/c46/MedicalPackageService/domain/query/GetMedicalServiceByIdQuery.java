package com.clinic.c46.MedicalPackageService.domain.query;


import lombok.Builder;

@Builder
public record GetMedicalServiceByIdQuery(String medicalServiceId) {
}
