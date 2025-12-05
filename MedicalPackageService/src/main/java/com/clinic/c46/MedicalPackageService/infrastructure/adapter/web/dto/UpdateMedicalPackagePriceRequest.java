package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateMedicalPackagePriceRequest {
    private BigDecimal price;
}
