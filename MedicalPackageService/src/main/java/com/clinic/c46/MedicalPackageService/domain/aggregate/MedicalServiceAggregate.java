package com.clinic.c46.MedicalPackageService.domain.aggregate;


import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalServiceInfoUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.Objects;

@Aggregate
@NoArgsConstructor
public class MedicalServiceAggregate {

    @AggregateIdentifier
    private String medicalServiceId;
    private String name;
    private String description;
    private String departmentId;
    private String formTemplate;
    private int processingPriority;

    @CommandHandler
    public MedicalServiceAggregate(CreateMedicalServiceCommand cmd) {
        if (cmd.name() == null || cmd.name()
                .isBlank()) {
            throw new IllegalArgumentException("Tên dịch vụ không được để trống");
        }

        MedicalServiceCreatedEvent event = MedicalServiceCreatedEvent.builder()
                .medicalServiceId(cmd.medicalServiceId())
                .name(cmd.name())
                .description(cmd.description())
                .departmentId(cmd.departmentId())
                .processingPriority(cmd.processingPriority())
                .formTemplate(cmd.formTemplate())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalServiceCreatedEvent event) {
        this.medicalServiceId = event.medicalServiceId();
        this.name = event.name();
        this.description = event.description();
        this.departmentId = event.departmentId();
        this.processingPriority = event.processingPriority();
        this.formTemplate = event.formTemplate();
    }

    @CommandHandler
    public void handle(UpdateMedicalServiceInfoCommand cmd) {
        boolean changed = false;

        if (cmd.name() != null && !cmd.name()
                .equals(this.name)) changed = true;
        if (cmd.description() != null && !cmd.description()
                .equals(this.description)) changed = true;
        if (cmd.departmentId() != null && !cmd.departmentId()
                .equals(this.departmentId)) changed = true;

        if (!(cmd.processingPriority() == (this.processingPriority))) changed = true;
        if (!(Objects.equals(cmd.formTemplate(), this.formTemplate))) changed = true;

        if (!changed) return;

        var event = MedicalServiceInfoUpdatedEvent.builder()
                .medicalServiceId(cmd.medicalServiceId())
                .name(cmd.name())
                .description(cmd.description())
                .departmentId(cmd.departmentId())
                .processingPriority(cmd.processingPriority())
                .formTemplate(cmd.formTemplate())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalServiceInfoUpdatedEvent event) {
        if (event.name() != null) this.name = event.name();
        if (event.description() != null) this.description = event.description();
        if (event.departmentId() != null) this.departmentId = event.departmentId();
        if (event.formTemplate() != null) this.formTemplate = event.formTemplate();
        this.processingPriority = event.processingPriority();
    }


}
