package com.clinic.c46.MedicalPackageService.application.dto;


import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record MedicalPackageDetailDTO(String medicalPackageId, String name, String description, BigDecimal price,
                                      List<MedicalServiceDetailsDTO> medicalServices, String image) {

}
