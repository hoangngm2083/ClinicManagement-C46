package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class UpdateMedicalPackageRequest {
    private String name;
    private String description;
    private String image;
    private Set<String> serviceIds;
    private BigDecimal price;
}
