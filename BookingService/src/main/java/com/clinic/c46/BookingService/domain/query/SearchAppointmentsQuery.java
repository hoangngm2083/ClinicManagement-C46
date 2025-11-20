package com.clinic.c46.BookingService.domain.query;

import java.time.LocalDate;

public record SearchAppointmentsQuery(String keyword, LocalDate dateFrom, LocalDate dateTo, String state, int page, String sortBy, String sort) {
}
