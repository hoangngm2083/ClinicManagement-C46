package com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CreateBookingRequest {
    private String slotId;
    private String name;
    private String email;
    private String phone;
}
