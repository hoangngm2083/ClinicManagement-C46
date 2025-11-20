package com.clinic.c46.BookingService.domain.query;


import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class SearchAppointmentsQuery {
    Integer page;
    String sortBy;
    String sort;
    String keyword;
    @Builder.Default
    String state = AppointmentState.CREATED.name();

    LocalDate dateFrom;
    LocalDate dateTo;

    @Builder.Default
    Integer size = 10;

    @Builder.Default
    Boolean includeDeleted = false;
}

