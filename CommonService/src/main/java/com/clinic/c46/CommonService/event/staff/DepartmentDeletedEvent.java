package com.clinic.c46.CommonService.event.staff;

import lombok.Builder;

@Builder
public record DepartmentDeletedEvent(String departmentId) {

}
