package com.clinic.c46.CommonService.event.staff;

import lombok.Builder;

@Builder
public record DepartmentUpdatedEvent(String departmentId, String name, String description

) {

}
