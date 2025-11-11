package com.clinic.c46.StaffService.application.dto;

import com.clinic.c46.CommonService.type.Shift;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Objects;

public record DateOffRequest(
        @NotNull(message = "Date is required") @FutureOrPresent(message = "Date must be today or in the future") LocalDate date,

        @NotNull(message = "Shift is required") Shift shift,

        @Size(max = 500, message = "Reason must not exceed 500 characters") String reason) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateOffRequest that = (DateOffRequest) o;
        return Objects.equals(date, that.date) && shift == that.shift;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, shift);
    }
}


