package com.clinic.c46.ExaminationService.domain.aggregate;

import com.clinic.c46.CommonService.command.examination.AddResultCommand;
import com.clinic.c46.CommonService.command.examination.CompleteExaminationCommand;
import com.clinic.c46.CommonService.command.examination.CreateExaminationCommand;
import com.clinic.c46.CommonService.event.examination.ExaminationCompletedEvent;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.CommonService.event.examination.ResultAddedEvent;
import com.clinic.c46.CommonService.exception.ResourceExistedException;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.ExaminationService.domain.command.DeleteExaminationCommand;
import com.clinic.c46.ExaminationService.domain.command.RemoveResultCommand;
import com.clinic.c46.ExaminationService.domain.event.ExaminationDeletedEvent;
import com.clinic.c46.ExaminationService.domain.event.ResultRemovedEvent;
import com.clinic.c46.ExaminationService.domain.valueObject.ExaminationStatus;
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
import org.axonframework.modelling.command.AggregateMember;
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
    private ExaminationStatus status;
    @AggregateMember(routingKey = "serviceId")
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
        this.status = ExaminationStatus.PENDING;
        this.results = new HashSet<>();
    }

    @CommandHandler
    public void handle(AddResultCommand cmd) {
        MedicalResult potentialNewResult = MedicalResult.builder()
                .serviceId(cmd.serviceId())
                .build();
        if (this.results.contains(potentialNewResult)) {
            throw new ResourceExistedException("Kết quả dịch vụ khám");
        }

        AggregateLifecycle.apply(new ResultAddedEvent(cmd.examId(), cmd.doctorId(), cmd.serviceId(), cmd.data()));
    }

    @EventSourcingHandler
    protected void on(ResultAddedEvent event) {

        MedicalResult newResult = MedicalResult.builder()
                .serviceId(event.serviceId())
                .doctorId(event.doctorId())
                .status(ResultStatus.CREATED)
                .build();
        this.results.add(newResult);
    }

    @CommandHandler
    public void handle(RemoveResultCommand cmd) {
        findResultOrThrow(cmd.serviceId());

        AggregateLifecycle.apply(new ResultRemovedEvent(cmd.examId(), cmd.serviceId()));
    }

    @EventSourcingHandler
    protected void on(ResultRemovedEvent event) {
        this.results.removeIf(r -> r.getServiceId()
                .equals(event.serviceId()));
    }

    @CommandHandler
    public void handle(DeleteExaminationCommand cmd) {
        // TODO: Check business rules before deleting (e.g., must not have SIGNED
        // results)
        if (this.results.stream()
                .anyMatch(r -> r.getStatus()
                        .equals(ResultStatus.SIGNED))) {
            throw new IllegalStateException("Không thể xóa hồ sơ khám có kết quả đã được ký.");
        }
        AggregateLifecycle.apply(new ExaminationDeletedEvent(cmd.examId()));
    }

    @EventSourcingHandler
    protected void on(ExaminationDeletedEvent event) {
        AggregateLifecycle.markDeleted();
    }

    @CommandHandler
    public void handle(CompleteExaminationCommand cmd) {
        if (this.status == ExaminationStatus.COMPLETED) {
            log.warn("Examination already completed: {}", cmd.examinationId());
            return;
        }

        AggregateLifecycle.apply(
                new ExaminationCompletedEvent(cmd.examinationId(), ExaminationStatus.COMPLETED.name()));
    }

    @EventSourcingHandler
    protected void on(ExaminationCompletedEvent event) {
        this.status = ExaminationStatus.valueOf(event.status());
    }

    private MedicalResult findResultOrThrow(String serviceId) {
        return this.results.stream()
                .filter(r -> r.getServiceId()
                        .equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Kết quả khám"));
    }
}