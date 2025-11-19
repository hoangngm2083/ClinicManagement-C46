package com.clinic.c46.ExaminationFlowService.domain.aggregate;

import com.clinic.c46.ExaminationFlowService.domain.command.CreateMedicalFormCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.MedicalFormCreatedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashSet;
import java.util.Set;


@Aggregate
@NoArgsConstructor
public class MedicalFormAggregate {
    @AggregateIdentifier
    private String medicalFormId;
    private String patientId;
    private String invoiceId;
    private String examinationId;
    private Set<String> packageIds = new HashSet<>();
    private MedicalFormStatus status;


    @CommandHandler
    public MedicalFormAggregate(CreateMedicalFormCommand cmd) {

        AggregateLifecycle.apply(MedicalFormCreatedEvent.builder()
                .medicalFormId(cmd.medicalFormId())
                .patientId(cmd.patientId())
                .invoiceId(cmd.invoiceId())
                .examinationId(cmd.examinationId())
                .packageIds(Set.copyOf(cmd.packageIds()))
                .status(MedicalFormStatus.CREATED.name())
                .build());
    }

    @EventSourcingHandler
    public void on(MedicalFormCreatedEvent event) {
        this.medicalFormId = event.medicalFormId();
        this.patientId = event.patientId();
        this.invoiceId = event.invoiceId();
        this.packageIds = Set.copyOf(event.packageIds());
        this.examinationId = event.examinationId();
        this.status = MedicalFormStatus.valueOf(event.status());
    }

}
