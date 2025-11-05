package com.clinic.c46.PatientService.domain.aggregate;

import com.clinic.c46.CommonService.command.patient.CreatePatientCommand;
import com.clinic.c46.CommonService.command.patient.DeletePatientCommand;
import com.clinic.c46.CommonService.event.patient.PatientCreatedEvent;
import com.clinic.c46.PatientService.domain.event.PatientDeletedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.Repository;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
public class PatientAggregate {
    private Repository<PatientAggregate> repository;

    @AggregateIdentifier
    private String patientId;
    private String email;
    private String name;
    private String phone;
    private boolean active;

    @CommandHandler
    public PatientAggregate(CreatePatientCommand command) {
        PatientCreatedEvent event = PatientCreatedEvent.builder()
                .patientId(command.patientId())
                .name(command.name())
                .phone(command.phone())
                .email(command.email())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(PatientCreatedEvent event) {
        this.patientId = event.patientId();
        this.email = event.email();
        this.name = event.name();
        this.phone = event.phone();
        this.active = true;
    }

    @CommandHandler
    public void handle(DeletePatientCommand command) {
        PatientDeletedEvent event = PatientDeletedEvent.builder()
                .patientId(command.patientId())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(PatientDeletedEvent event) {
        this.active = false;
    }

}
