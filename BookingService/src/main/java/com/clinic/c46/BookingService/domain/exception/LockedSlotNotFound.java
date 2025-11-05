package com.clinic.c46.BookingService.domain.exception;


public class LockedSlotNotFound extends RuntimeException {
    public LockedSlotNotFound(String message) {
        super(message);
    }

    public LockedSlotNotFound() {
        super("Locked Slot Not Found!");
    }
}
