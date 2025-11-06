package com.clinic.c46.BookingService.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CreateSlotsRequest {
    List<CreateSlotRequest> slots;
}
