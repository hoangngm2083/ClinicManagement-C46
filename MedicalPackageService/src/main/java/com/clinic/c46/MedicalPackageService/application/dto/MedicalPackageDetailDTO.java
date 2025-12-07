package com.clinic.c46.MedicalPackageService.application.dto;


import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record MedicalPackageDetailDTO(String medicalPackageId, String name, String description, 
                                      Map<Integer, BigDecimal> prices,
                                      List<MedicalServiceDetailsDTO> medicalServices, String image) {

}
