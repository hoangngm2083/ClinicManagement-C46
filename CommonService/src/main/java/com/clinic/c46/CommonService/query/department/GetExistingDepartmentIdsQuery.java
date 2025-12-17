package com.clinic.c46.CommonService.query.department;


import lombok.Builder;

import java.util.Set;

@Builder
public record GetExistingDepartmentIdsQuery(Set<String> departmentIds) {
}
