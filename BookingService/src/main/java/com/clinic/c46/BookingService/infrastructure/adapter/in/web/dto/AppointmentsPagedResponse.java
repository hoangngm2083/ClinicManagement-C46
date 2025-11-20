package com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto;

import com.clinic.c46.BookingService.application.dto.AppointmentDto;
import com.clinic.c46.CommonService.dto.BasePagedResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor(force = true)
@SuperBuilder
public class AppointmentsPagedResponse extends BasePagedResponse<AppointmentDto> {
}
