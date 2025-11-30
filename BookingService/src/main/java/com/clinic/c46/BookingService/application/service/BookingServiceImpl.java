package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.application.dto.AppointmentDetailsDto;
import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import com.clinic.c46.BookingService.domain.command.UpdateAppointmentStateCommand;
import com.clinic.c46.BookingService.domain.query.ExistsBySlotIdQuery;
import com.clinic.c46.BookingService.domain.query.GetAppointmentByIdQuery;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.patient.ExistsPatientByIdQuery;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final CommandGateway commandGateway;

    private final QueryGateway queryGateway;

    private final AppointmentViewRepository appointmentViewRepository;

    @Override
    public void lockSlot(LockSlotCommand cmd) {
        commandGateway.send(cmd);
    }

    @Override
    public CompletableFuture<Void> createAppointment(CreateAppointmentCommand cmd) {

        CompletableFuture<Void> patientCheck = queryGateway.query(new ExistsPatientByIdQuery(cmd.patientId()),
                        ResponseTypes.instanceOf(Boolean.class))
                .thenAccept((isExists) -> {
                    if (isExists.equals(Boolean.FALSE)) {
                        throw new ResourceNotFoundException("Bệnh nhân");
                    }
                });

        CompletableFuture<Void> slotCheck = queryGateway.query(new ExistsBySlotIdQuery(cmd.slotId()),
                        ResponseTypes.instanceOf(Boolean.class))
                .thenAccept((isExists) -> {
                    if (isExists.equals(Boolean.FALSE)) {
                        throw new IllegalArgumentException("Suất khám");
                    }
                });

        return patientCheck.thenCombine(slotCheck, (a, b) -> null)
                .thenCompose(v -> commandGateway.send(cmd));
    }

    @Override
    public CompletableFuture<Object> updateAppointmentState(UpdateAppointmentStateCommand cmd) {
        return commandGateway.send(cmd);
    }

    @Override
    public CompletableFuture<Optional<AppointmentDetailsDto>> getAppointmentById(String appointmentId) {
        GetAppointmentByIdQuery query = new GetAppointmentByIdQuery(appointmentId);
        return queryGateway.query(query, ResponseTypes.optionalInstanceOf(AppointmentDetailsDto.class));
    }

    @Override
    public void deleteAppointment(String appointmentId) {
        appointmentViewRepository.deleteById(appointmentId);
    }

}


