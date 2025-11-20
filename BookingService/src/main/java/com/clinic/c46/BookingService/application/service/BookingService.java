package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.domain.command.CancelAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.view.AppointmentView;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BookingService {
    void lockSlot(LockSlotCommand command);

    CompletableFuture<Object> createAppointment(CreateAppointmentCommand command);

    CompletableFuture<Object> cancelAppointment(CancelAppointmentCommand command);

    Optional<AppointmentView> getAppointmentById(String appointmentId);

    AppointmentView saveAppointmentView(AppointmentView appointmentView);

    void deleteAppointment(String appointmentId);

}
