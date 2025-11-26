//package com.clinic.c46.ExaminationFlowService.application.saga;
//
//import com.clinic.c46.CommonService.event.payment.TransactionCompletedEvent;
//import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
//import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
//import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemResponse;
//import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemResponseByIdQuery;
//import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
//import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
//import com.clinic.c46.ExaminationFlowService.domain.command.CompleteQueueItemCommand;
//import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCompletedEvent;
//import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//import org.axonframework.commandhandling.gateway.CommandGateway;
//import org.axonframework.messaging.responsetypes.ResponseTypes;
//import org.axonframework.modelling.saga.EndSaga;
//import org.axonframework.modelling.saga.SagaEventHandler;
//import org.axonframework.modelling.saga.SagaLifecycle;
//import org.axonframework.modelling.saga.StartSaga;
//import org.axonframework.queryhandling.QueryGateway;
//import org.axonframework.spring.stereotype.Saga;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.Optional;
//
///**
// * @deprecated Use {@link QueueItemProcessingSaga} instead.
// *             This saga is replaced by unified QueueItemProcessingSaga that
// *             handles both
// *             EXAM_SERVICE and RECEPTION_PAYMENT flows.
// *
// *             ReceptionPaymentProcessingSaga xử lý quy trình thanh toán tại bàn
// *             lễ tân.
// *
// *             Luồng xử lý:
// *             1. Bắt đầu khi QueueItemTakenEvent với type = RECEPTION_PAYMENT
// *             2. Lắng nghe sự kiện TransactionCompletedEvent từ PaymentService
// *             3. Hoàn thành QueueItem khi thanh toán thành công
// */
//@Deprecated(forRemoval = true, since = "2.0")
//@Saga
//@NoArgsConstructor
//@Slf4j
//@Getter
//@Setter
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class ReceptionPaymentProcessingSaga {
//
//    @Autowired
//    @JsonIgnore
//    private transient QueryGateway queryGateway;
//
//    @Autowired
//    @JsonIgnore
//    private transient CommandGateway commandGateway;
//
//    @Autowired
//    @JsonIgnore
//    private transient WebSocketNotifier wSNotifier;
//
//    private String staffId;
//    private String queueId;
//    private String queueItemId;
//    private String invoiceId;
//
//    private ReceptionPaymentProcessingSagaStateMachine stateMachine;
//
//    /**
//     * Bắt đầu Saga khi QueueItemTakenEvent được phát sinh cho mục thanh toán
//     * (RECEPTION_PAYMENT)
//     */
//    @StartSaga
//    @SagaEventHandler(associationProperty = "queueItemId")
//    public void on(QueueItemTakenEvent event) {
//        try {
//            log.info("[ReceptionPaymentProcessingSaga] START: Received QueueItemTakenEvent. " +
//                    "QueueItemId: {}, StaffId: {}, Type: {}",
//                    event.queueItemId(), event.staffId(), event.type());
//
//            // ✅ KIỂM TRA LOẠI QUEUE ITEM NGAY TỪ EVENT
//            // Nếu không phải RECEPTION_PAYMENT, kết thúc saga ngay (để
//            // ExamWorkFlowProcessingSaga xử lý)
//            if (event.type() != QueueItemType.RECEPTION_PAYMENT) {
//                log.debug("[ReceptionPaymentProcessingSaga] Skipping non-payment QueueItem {}. " +
//                        "Type: {}. This will be handled by ExamWorkFlowProcessingSaga.",
//                        event.queueItemId(), event.type());
//                SagaLifecycle.end();
//                return;
//            }
//
//            this.queueId = event.queueId();
//            this.staffId = event.staffId();
//            this.queueItemId = event.queueItemId();
//
//            // Liên kết Saga với staffId để xử lý sự kiện thanh toán
//            SagaLifecycle.associateWith("staffId", event.staffId());
//
//            this.stateMachine = ReceptionPaymentProcessingSagaStateMachine.PAYMENT_PENDING;
//
//            log.info("[ReceptionPaymentProcessingSaga] Confirmed RECEPTION_PAYMENT queue item. " +
//                    "QueueItemId: {}", event.queueItemId());
//
//            // Lấy thông tin chi tiết của queue item để lấy invoiceId
//            GetQueueItemResponseByIdQuery query = new GetQueueItemResponseByIdQuery(event.queueItemId());
//            Optional<QueueItemResponse> queueItemResponseOpt = queryGateway.query(query,
//                    ResponseTypes.optionalInstanceOf(QueueItemResponse.class))
//                    .join();
//
//            QueueItemResponse queueItem = require(queueItemResponseOpt,
//                    "Phiếu chờ thanh toán không tồn tại");
//
//            // Với RECEPTION_PAYMENT type, invoiceId sẽ được query từ PaymentService hoặc MedicalFormService
//            // Tạm thời sử dụng queueItemId làm reference identifier để associate saga
//            this.invoiceId = event.queueItemId();
//
//            // Liên kết Saga với invoiceId
//            SagaLifecycle.associateWith("invoiceId", this.invoiceId);
//
//            // Gửi thông báo cho nhân viên lễ tân
//            wSNotifier.sendToUser(event.staffId(), queueItem);
//            wSNotifier.broadcast(event.queueId(), getQueueSize(event.queueId()));
//
//            this.stateMachine = ReceptionPaymentProcessingSagaStateMachine.WAITING_FOR_PAYMENT;
//            log.info("[ReceptionPaymentProcessingSaga] Status changed to WAITING_FOR_PAYMENT. " +
//                    "InvoiceId: {}", this.invoiceId);
//
//        } catch (Exception e) {
//            log.error("[ReceptionPaymentProcessingSaga.on(QueueItemTakenEvent)] EXCEPTION occurred: {}",
//                    e.getClass().getSimpleName());
//            log.error("[ReceptionPaymentProcessingSaga.on(QueueItemTakenEvent)] Exception message: {}",
//                    e.getMessage());
//            log.error("[ReceptionPaymentProcessingSaga.on(QueueItemTakenEvent)] Full exception: ", e);
//            handleException(e);
//        }
//    }
//
//    /**
//     * Lắng nghe sự kiện TransactionCompletedEvent từ PaymentService
//     * Khi thanh toán thành công, hoàn thành queue item
//     */
//    @SagaEventHandler(associationProperty = "invoiceId")
//    public void handle(TransactionCompletedEvent event) {
//        try {
//            log.info("[ReceptionPaymentProcessingSaga] Received TransactionCompletedEvent. " +
//                    "InvoiceId: {}, TransactionId: {}", event.invoiceId(), event.transactionId());
//
//            this.stateMachine = ReceptionPaymentProcessingSagaStateMachine.PAYMENT_COMPLETED;
//
//            // Gửi lệnh hoàn thành queue item
//            CompleteQueueItemCommand cmd = new CompleteQueueItemCommand(queueItemId, staffId);
//            commandGateway.send(cmd)
//                    .whenComplete((result, throwable) -> {
//                        if (throwable != null) {
//                            log.error("[ReceptionPaymentProcessingSaga] Error completing queue item: {}",
//                                    throwable.getMessage());
//                        } else {
//                            log.info("[ReceptionPaymentProcessingSaga] Queue item completed successfully. " +
//                                    "QueueItemId: {}", queueItemId);
//                        }
//                    });
//
//        } catch (Exception e) {
//            log.error("[ReceptionPaymentProcessingSaga.handle(TransactionCompletedEvent)] EXCEPTION: {}",
//                    e.getMessage(), e);
//            handleException(e);
//        }
//    }
//
//    /**
//     * Kết thúc Saga khi QueueItemCompletedEvent được phát sinh
//     */
//    @EndSaga
//    @SagaEventHandler(associationProperty = "queueItemId")
//    public void handle(QueueItemCompletedEvent event) {
//        this.stateMachine = ReceptionPaymentProcessingSagaStateMachine.COMPLETED;
//        log.info("[ReceptionPaymentProcessingSaga] Saga ended. QueueItemId: {}", event.queueItemId());
//    }
//
//    private <T> T require(Optional<T> opt, String message) {
//        return opt.orElseThrow(() -> new ResourceNotFoundException(message));
//    }
//
//    private Long getQueueSize(String queueId) {
//        try {
//            log.debug("[ReceptionPaymentProcessingSaga.getQueueSize] Getting queue size for queueId={}", queueId);
//            Long size = queryGateway.query(new GetQueueSizeQuery(queueId),
//                    ResponseTypes.instanceOf(Long.class))
//                    .join();
//            log.debug("[ReceptionPaymentProcessingSaga.getQueueSize] Queue size: {} for queueId={}",
//                    size, queueId);
//            return size;
//        } catch (Exception e) {
//            log.warn("[ReceptionPaymentProcessingSaga.getQueueSize] Error getting queue size: {}",
//                    e.getMessage());
//            return 0L;
//        }
//    }
//
//    private void handleException(Throwable throwable) {
//        log.warn("[ReceptionPaymentProcessingSaga.handleException] Exception in state {}: {}",
//                this.stateMachine, throwable.getMessage());
//        log.warn("[ReceptionPaymentProcessingSaga.handleException] Exception class: {}",
//                throwable.getClass().getName());
//
//        if (this.staffId != null) {
//            try {
//                wSNotifier.notifyErrorToUser(this.staffId,
//                        "Lỗi xử lý thanh toán: " + throwable.getMessage());
//            } catch (Exception e) {
//                log.error("[ReceptionPaymentProcessingSaga.handleException] Error notifying user: {}",
//                        e.getMessage());
//            }
//        }
//
//        log.info("[ReceptionPaymentProcessingSaga.handleException] Ending saga");
//        SagaLifecycle.end();
//    }
//}
