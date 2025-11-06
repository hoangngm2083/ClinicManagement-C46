package com.clinic.c46.BookingService.application.handler.query;

import com.clinic.c46.BookingService.application.port.out.BookingStatusViewRepository;
import com.clinic.c46.BookingService.domain.query.GetBookingStatusByIdQuery;
import com.clinic.c46.BookingService.domain.view.BookingStatusView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BookingQueryHandler {
    private final BookingStatusViewRepository bookingStatusViewRepository;

    @QueryHandler
    public BookingStatusView handle(GetBookingStatusByIdQuery query) {
        return bookingStatusViewRepository.findById(query.bookingId())
                .orElse(null);
    }

}
