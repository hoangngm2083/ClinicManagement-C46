package com.clinic.c46.CommonService.query.medicalPackage;


import lombok.Builder;

@Builder
public record GetAllPackagesQuery(int page, String keyword) {
}
