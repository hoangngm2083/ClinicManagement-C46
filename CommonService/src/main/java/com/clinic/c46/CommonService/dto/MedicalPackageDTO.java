package com.clinic.c46.CommonService.dto;


import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MedicalPackageDTO(String medicalPackageId, String name, String description, BigDecimal price,
                                int priceVersion, String image) {

}
