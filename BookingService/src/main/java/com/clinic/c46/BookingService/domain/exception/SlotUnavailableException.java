package com.clinic.c46.BookingService.domain.exception;

import com.clinic.c46.CommonService.exception.BaseDomainException;

public class SlotUnavailableException extends BaseDomainException {
    public SlotUnavailableException() {
        this.message = "Slot unavailable!";
    }

    public SlotUnavailableException(String message) {
        this.message = message;
    }

}
