package com.clinic.c46.BookingService.domain.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AppointmentView {
    @Id
    private String id;
}
