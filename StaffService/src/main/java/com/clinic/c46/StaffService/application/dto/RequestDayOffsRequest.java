package com.clinic.c46.StaffService.application.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;


public record RequestDayOffsRequest(
        @NotEmpty
        Set<@Valid DateOffRequest> dayOffs) {
}
