package com.clinic.c46.BookingService.infrastructure.adapter.in;

import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingQueryHandler {

    private final QueryGateway queryGateway;
}
