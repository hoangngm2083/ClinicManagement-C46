package com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSlotMaxQuantityRequest {
    private int maxQuantity;
}
