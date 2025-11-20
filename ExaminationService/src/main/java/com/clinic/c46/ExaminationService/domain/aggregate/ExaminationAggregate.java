package com.clinic.c46.ExaminationService.domain.aggregate;

import com.clinic.c46.CommonService.command.examination.CreateExaminationCommand;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.ExaminationService.domain.command.*;
import com.clinic.c46.ExaminationService.domain.event.*;
import com.clinic.c46.ExaminationService.domain.valueObject.MedicalResult;
import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Aggregate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ExaminationAggregate {

    @AggregateIdentifier
    private String examinationId;
    private String patientId;
    private Set<MedicalResult> results = new HashSet<>();

    @CommandHandler
    public ExaminationAggregate(CreateExaminationCommand cmd) {
        AggregateLifecycle.apply(new ExaminationCreatedEvent(cmd.examinationId(), cmd.patientId(), cmd.medicalFormId()));
    }

    @EventSourcingHandler
    protected void on(ExaminationCreatedEvent event) {
        this.examinationId = event.examinationId();
        this.patientId = event.patientId();
        this.results = new HashSet<>();
    }

    @CommandHandler
    public void handle(AddResultCommand cmd) {
        // TODO: Validate result data, schema, etc.
        if (this.results.contains(cmd.medicalResult())) {
            log.warn("examination.add-result.command Medical Result already exists");
            return; // Impotency, improve by use version
        }
        AggregateLifecycle.apply(new ResultAddedEvent(cmd.examId(), cmd.medicalResult()));
    }

    @EventSourcingHandler
    protected void on(ResultAddedEvent event) {
        this.results.add(event.medicalResult());
    }

    @CommandHandler
    public void handle(RemoveResultCommand cmd) {
        // Validate result exists
        findResultOrThrow(cmd.serviceId());
        AggregateLifecycle.apply(new ResultRemovedEvent(cmd.examId(), cmd.serviceId()));
    }

    @EventSourcingHandler
    protected void on(ResultRemovedEvent event) {
        this.results.removeIf(r -> r.getServiceId()
                .equals(event.serviceId()));
    }

    @CommandHandler
    public void handle(UpdateResultStatusCommand cmd) {
        MedicalResult resultToUpdate = findResultOrThrow(cmd.serviceId());

        // Only allow update if current status is not SIGNED
        if (resultToUpdate.getStatus().equals(ResultStatus.SIGNED)) {
            log.warn("examination.update-result.command Result with status SIGNED cannot be updated");
            return;
        }

        ResultStatus newStatus = ResultStatus.valueOf(cmd.newStatus());
        if (resultToUpdate.getStatus().equals(newStatus)) {
            return;
        }

        AggregateLifecycle.apply(new ResultStatusUpdatedEvent(cmd.examId(), cmd.serviceId(), newStatus));
    }

    @EventSourcingHandler
    protected void on(ResultStatusUpdatedEvent event) {
        findResultOrThrow(event.serviceId()).setStatus(event.newStatus());
    }


    @CommandHandler
    public void handle(DeleteExaminationCommand cmd) {
        // TODO: Check business rules before deleting
        AggregateLifecycle.apply(new ExaminationDeletedEvent(cmd.examId()));
    }

    @EventSourcingHandler
    protected void on(ExaminationDeletedEvent event) {
        AggregateLifecycle.markDeleted();
    }

    private MedicalResult findResultOrThrow(MedicalResult result) {
        return this.results.stream()
                .filter(r -> r.equals(result))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Result with serviceId " + result.getServiceId() + " not found."));
    }

    private MedicalResult findResultOrThrow(String serviceId) {
        return this.results.stream()
                .filter(r -> r.getServiceId()
                        .equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Result with serviceId " + serviceId + " not found."));
    }
}
