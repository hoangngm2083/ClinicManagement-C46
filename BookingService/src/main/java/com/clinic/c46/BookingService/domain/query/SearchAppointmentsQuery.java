package com.clinic.c46.BookingService.domain.query;


import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.CommonService.helper.SortDirection;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class SearchAppointmentsQuery {
    @Builder.Default
    Integer page = 0;
    @Builder.Default

    String sortBy = "createdAt";
    @Builder.Default

    String sort = SortDirection.ASC.name();

    String keyword;
    @Builder.Default
    String state = AppointmentState.CREATED.name();
    @Builder.Default

    LocalDate dateFrom = LocalDate.now();
    @Builder.Default

    LocalDate dateTo = LocalDate.now();

    @Builder.Default
    Integer size = 10;

    @Builder.Default
    Boolean includeDeleted = false;
}

