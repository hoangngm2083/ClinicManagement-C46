package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateMedicalServiceRequest {
    private String medicalServiceId;
    private String name;
    private String description;
    private String departmentId;
}
