package com.clinic.c46.BookingService.infrastructure.adapter.in;


import com.clinic.c46.BookingService.application.port.out.BookingStatusViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventListener {
    private final BookingStatusViewRepository bookingStatusViewRepository;
}
