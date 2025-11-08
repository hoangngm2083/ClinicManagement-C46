package com.clinic.c46.BookingService.domain.aggregate;


import com.clinic.c46.BookingService.domain.command.CancelAppointmentCommand;
import com.clinic.c46.BookingService.domain.command.CreateAppointmentCommand;
import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.BookingService.domain.event.AppointmentCanceledEvent;
import com.clinic.c46.BookingService.domain.event.AppointmentCreatedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@NoArgsConstructor
@Aggregate
public class AppointmentAggregate {
    @AggregateIdentifier
    private String appointmentId;
    private String patientId;
    private String slotId;
    private AppointmentState state;


    @CommandHandler
    public AppointmentAggregate(CreateAppointmentCommand cmd) {
        AppointmentCreatedEvent appointmentCreatedEvent = AppointmentCreatedEvent.builder()
                .appointmentId(cmd.appointmentId())
                .patientId(cmd.patientId())
                .slotId(cmd.slotId())
                .build();

        AggregateLifecycle.apply(appointmentCreatedEvent);
    }

    @EventSourcingHandler
    public void on(AppointmentCreatedEvent event) {
        this.appointmentId = event.appointmentId();
        this.patientId = event.patientId();
        this.slotId = event.slotId();
        this.state = AppointmentState.CREATED;
    }

    @CommandHandler
    public void handle(CancelAppointmentCommand cmd) {

        AggregateLifecycle.apply(AppointmentCanceledEvent.builder()
                .appointmentId(this.appointmentId)
                .build());
    }

    @EventSourcingHandler
    public void on(AppointmentCanceledEvent event) {
        this.state = AppointmentState.CANCELED;
    }

}
