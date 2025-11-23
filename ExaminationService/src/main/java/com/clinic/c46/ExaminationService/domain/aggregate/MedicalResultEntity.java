package com.clinic.c46.ExaminationService.domain.aggregate;


import com.clinic.c46.CommonService.event.examination.ResultAddedEvent;
import com.clinic.c46.ExaminationService.domain.command.UpdateResultStatusCommand;
import com.clinic.c46.ExaminationService.domain.event.ResultStatusUpdatedEvent;
import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import lombok.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.EntityId;

/**
 * MedicalResult là một Aggregate Member Entity, có vòng đời gắn liền với ExaminationAggregate.
 * Nó chịu trách nhiệm xử lý các Command nhắm vào ID riêng của mình (serviceId).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "serviceId")
public class MedicalResultEntity {

    @EntityId
    private String serviceId;
    private String doctorId;
    private ResultStatus status;


    @CommandHandler
    public void handle(UpdateResultStatusCommand cmd) {
        if (this.status.equals(ResultStatus.SIGNED)) {
            throw new IllegalStateException("Không thể cập nhật sau khi ký!");
        }

        ResultStatus newStatus = ResultStatus.valueOf(cmd.newStatus());
        if (this.status.equals(newStatus)) {
            return;
        }

        AggregateLifecycle.apply(new ResultStatusUpdatedEvent(cmd.examId(), cmd.serviceId(), newStatus));
    }

    @EventSourcingHandler
    public void on(ResultStatusUpdatedEvent event) {
        this.status = event.newStatus();
    }

    @EventSourcingHandler
    public void on(ResultAddedEvent event) {
        this.serviceId = event.serviceId();
        this.doctorId = event.doctorId();
        this.status = ResultStatus.CREATED;
    }
}