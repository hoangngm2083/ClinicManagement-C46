package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.domain.command.LockSlotCommand;

public interface BookingService {
    void lockSlot(LockSlotCommand command);
}
