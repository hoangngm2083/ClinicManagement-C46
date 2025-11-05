package com.clinic.c46.MedicalPackageService.application.handler.query.dto;


import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record MedicalPackageDTO(
        String medicalPackageId,
        String name,
        String description,
        BigDecimal price,
        List<MedicalServiceDTO> medicalServices

) {

}
