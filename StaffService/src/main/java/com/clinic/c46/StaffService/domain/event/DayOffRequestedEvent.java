package com.clinic.c46.StaffService.domain.event;

import com.clinic.c46.StaffService.domain.valueObject.DayOff;
import lombok.Builder;

import java.util.Set;


@Builder
public record DayOffRequestedEvent(String staffId, Set<DayOff> dayOffs) {
}
