package com.clinic.c46.CommonService.query.medicalPackage;


import com.clinic.c46.CommonService.helper.SortDirection;
import lombok.Builder;

@Builder
public record GetAllPackagesQuery(int page, String keyword, SortDirection sort) {
}
