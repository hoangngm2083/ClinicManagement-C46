package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateMedicalPackageRequest {
    private String name;
    private String description;
    private Set<String> serviceIds;
    private BigDecimal price;
    private String image;
}
