package com.clinic.c46.StaffService.domain.query;

import lombok.Builder;

@Builder
public record FindStaffScheduleQuery(int month, int year) {
}
