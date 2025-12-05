package com.clinic.c46.StaffService.domain.query;

import lombok.Builder;

@Builder
public record GetAllDepartmentsQuery(String keyword, int page, int size) {
}
