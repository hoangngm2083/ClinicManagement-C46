package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.application.dto.AppointmentDetailsDto;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.command.UpdateAppointmentStateCommand;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BookingService {
    void lockSlot(LockSlotCommand command);

    CompletableFuture<Object> createAppointment(CreateAppointmentCommand command);

    CompletableFuture<Object> updateAppointmentState(UpdateAppointmentStateCommand command);

    CompletableFuture<Optional<AppointmentDetailsDto>> getAppointmentById(String appointmentId);

    void deleteAppointment(String appointmentId);

}
