package com.clinic.c46.ExaminationFlowService.domain.aggregate;

import com.clinic.c46.ExaminationFlowService.domain.command.CreateQueueItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.command.TakeNextItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.clinic.c46.ExaminationFlowService.domain.exception.TakeItemConflictException;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
@ToString
public class QueueItemAggregate {
    @AggregateIdentifier
    private String queueItemId;
    private String medicalFormId;
    private String serviceId;
    private String queueId; // Department ID
    private String staffId;
    private QueueItemStatus status; // WAITING, IN_PROGRESS, COMPLETED

    @CommandHandler
    public QueueItemAggregate(CreateQueueItemCommand cmd) {
        AggregateLifecycle.apply(
                new QueueItemCreatedEvent(cmd.queueItemId(), cmd.medicalFormId(), cmd.serviceId(), cmd.queueId(),
                        QueueItemStatus.WAITING.name()));
    }

    @EventSourcingHandler
    public void on(QueueItemCreatedEvent evt) {
        this.queueItemId = evt.queueItemId();
        this.medicalFormId = evt.medicalFormId();
        this.serviceId = evt.serviceId();
        this.queueId = evt.queueId();
        this.status = QueueItemStatus.valueOf(evt.status());
    }

    @CommandHandler
    public void handle(TakeNextItemCommand cmd) {
        if (!QueueItemStatus.IN_PROGRESS.equals(this.status)) {
            throw new TakeItemConflictException("This medical form is being processed.");
        }
        // TODO: apply event
        AggregateLifecycle.apply(QueueItemTakenEvent.builder()
                .queueItemId(cmd.queueItemId())
                .queueId(this.queueId)
                .staffId(cmd.staffId())
                .build());
    }

    @EventSourcingHandler
    public void on(QueueItemTakenEvent evt) {
        this.staffId = evt.staffId();
        this.status = QueueItemStatus.IN_PROGRESS;
    }

//    // 3. Lệnh "Bác sĩ yêu cầu thêm dịch vụ" (Re-schedule)
//    @CommandHandler
//    public void handle(RequestSupplementalServiceCommand cmd) {
//        // ... (Kiểm tra logic)
//        apply(new SupplementalServiceRequestedEvent(this.queueItemId, cmd.getNewServiceId(), cmd.getNewServiceRoomId(),
//                // Phòng ban của dịch vụ mới
//                this.currentRoom // Phòng ban hiện tại để quay lại
//        ));
//
//        // Cập nhật trạng thái của phiếu hiện tại
//        apply(new QueueItemRescheduledEvent(this.queueItemId, "PENDING_SUPPLEMENT"));
//    }
//
//    // 4. Lệnh "Hoàn thành khám"
//    @CommandHandler
//    public void handle(CompleteQueueItemCommand cmd) {
//        if (!"IN_PROGRESS".equals(this.status)) {
//            throw new IllegalStateException("Không thể hoàn thành phiếu khám chưa được xử lý.");
//        }
//        apply(new QueueItemCompletedEvent(this.queueItemId, cmd.getResultData(), // Kết quả khám (JSON/Text)
//                this.doctorId));
//    }
//
//    @EventSourcingHandler
//    public void on(QueueItemCompletedEvent evt) {
//        this.status = "PENDING_PAYMENT";
//    }
}
