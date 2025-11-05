package com.clinic.c46.BookingService.domain.query;


import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class FindPatientBookingInProgressQuery {
    private String fingerprint;
}
