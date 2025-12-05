package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;

import lombok.Data;

import java.util.Set;

@Data
public class UpdateMedicalPackageInfoRequest {
    private String name;
    private String description;
    private String image;
    private Set<String> serviceIds;
}
