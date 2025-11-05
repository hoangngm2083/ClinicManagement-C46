package com.clinic.c46.BookingService.application.port.in;

import com.clinic.c46.BookingService.domain.command.LockSlotCommand;

public interface BookingService {
    void lockSlot(LockSlotCommand command);
}
