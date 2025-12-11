package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.CreateSlotRequest;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.UpdateSlotMaxQuantityRequest;

import java.util.concurrent.CompletableFuture;

public interface SlotService {
    CompletableFuture<String> create(CreateSlotRequest request);

    CompletableFuture<Void> update(String slotId, UpdateSlotMaxQuantityRequest request);
}
