package com.clinic.c46.BookingService.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
public class AppointmentDetailsDto extends AppointmentDto {
    private Set<ServiceDto> services;
}
