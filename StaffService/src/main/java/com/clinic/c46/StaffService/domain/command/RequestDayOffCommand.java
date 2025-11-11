package com.clinic.c46.StaffService.domain.command;

import com.clinic.c46.StaffService.domain.valueObject.DayOff;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Set;

@Builder
public record RequestDayOffCommand(@TargetAggregateIdentifier String staffId, Set<DayOff> dayOffs) {
}
