package com.clinic.c46.BookingService.domain.exception;

import com.clinic.c46.CommonService.exception.BaseDomainException;

public class SlotLockConflictException extends BaseDomainException {

    public SlotLockConflictException(){
        this.message = "Slot lock conflict!";
    }
    public SlotLockConflictException(String message) {
       this.message = message;
    }
}
