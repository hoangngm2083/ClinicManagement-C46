package com.clinic.c46.ExaminationService.domain.aggregate;

import com.clinic.c46.CommonService.command.examination.AddResultCommand;
import com.clinic.c46.CommonService.command.examination.CreateExaminationCommand;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.CommonService.event.examination.ResultAddedEvent;
import com.clinic.c46.CommonService.exception.ResourceExistedException;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.ExaminationService.domain.command.DeleteExaminationCommand;
import com.clinic.c46.ExaminationService.domain.command.RemoveResultCommand;
import com.clinic.c46.ExaminationService.domain.command.UpdateResultStatusCommand;
import com.clinic.c46.ExaminationService.domain.event.ExaminationDeletedEvent;
import com.clinic.c46.ExaminationService.domain.event.ResultRemovedEvent;
import com.clinic.c46.ExaminationService.domain.event.ResultStatusUpdatedEvent;
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
        AggregateLifecycle.apply(
                new ExaminationCreatedEvent(cmd.examinationId(), cmd.patientId(), cmd.medicalFormId()));
    }

    @EventSourcingHandler
    protected void on(ExaminationCreatedEvent event) {
        this.examinationId = event.examinationId();
        this.patientId = event.patientId();
        this.results = new HashSet<>();
    }

    @CommandHandler
    public void handle(AddResultCommand cmd) {
        MedicalResult medicalResult = MedicalResult.builder()
                .serviceId(cmd.serviceId())
                .build();
        if (this.results.contains(medicalResult)) {
            throw new ResourceExistedException("Kết quả dịch vụ khám");
        }
        AggregateLifecycle.apply(new ResultAddedEvent(cmd.examId(), cmd.doctorId(), cmd.serviceId(), cmd.data()));
    }

    @EventSourcingHandler
    protected void on(ResultAddedEvent event) {
        MedicalResult medicalResult = MedicalResult.builder()
                .doctorId(event.doctorId())
                .serviceId(event.serviceId())
                .status(ResultStatus.CREATED)
                .build();
        this.results.add(medicalResult);
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
        if (resultToUpdate.getStatus()
                .equals(ResultStatus.SIGNED)) {
            log.warn("examination.update-result.command Result with status SIGNED cannot be updated");
            return;
        }

        ResultStatus newStatus = ResultStatus.valueOf(cmd.newStatus());
        if (resultToUpdate.getStatus()
                .equals(newStatus)) {
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


    private MedicalResult findResultOrThrow(String serviceId) {
        return this.results.stream()
                .filter(r -> r.getServiceId()
                        .equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả khám"));
    }
}
