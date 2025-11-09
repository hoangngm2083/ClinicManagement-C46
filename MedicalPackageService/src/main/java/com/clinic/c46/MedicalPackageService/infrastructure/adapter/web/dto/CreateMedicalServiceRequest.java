package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateMedicalServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotBlank(message = "Mã khoa không được để trống")
    private String departmentId;
}

