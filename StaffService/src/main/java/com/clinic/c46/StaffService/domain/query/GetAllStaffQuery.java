package com.clinic.c46.StaffService.domain.query;

import com.clinic.c46.CommonService.helper.SortDirection;
import lombok.Builder;

@Builder
public record GetAllStaffQuery(String keyword, String departmentId, Integer role, int page, String sortBy,
                               SortDirection sort) {
}
