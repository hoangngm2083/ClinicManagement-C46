//package com.clinic.c46.ExaminationFlowService.application.saga;
//
//import com.clinic.c46.CommonService.event.examination.ResultAddedEvent;
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
//import org.axonframework.commandhandling.CommandExecutionException;
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
//import java.util.concurrent.CompletionException;
//
///**
// * @deprecated Use {@link QueueItemProcessingSaga} instead.
// *             This saga is replaced by unified QueueItemProcessingSaga that
// *             handles both
// *             EXAM_SERVICE and RECEPTION_PAYMENT flows.
// */
//@Deprecated(forRemoval = true, since = "2.0")
//@Saga
//@NoArgsConstructor
//@Slf4j
//@Getter
//@Setter
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class ExamWorkFlowProcessingSaga {
//    @Autowired
//    @JsonIgnore
//    private transient QueryGateway queryGateway;
//    @Autowired
//    @JsonIgnore
//    private transient CommandGateway commandGateway;
//    @Autowired
//    @JsonIgnore
//    private transient WebSocketNotifier wSNotifier;
//    private String staffId;
//    private String queueId;
//    private String queueItemId;
//
//    private ExamWorkFlowProcessingStateMachine stateMachine;
//
//    @StartSaga
//    @SagaEventHandler(associationProperty = "queueItemId")
//    public void on(QueueItemTakenEvent event) {
//        try {
//            this.queueId = event.queueId();
//            this.staffId = event.staffId();
//            this.queueItemId = event.queueItemId();
//            SagaLifecycle.associateWith("staffId", event.staffId());
//            SagaLifecycle.associateWith("doctorId", event.staffId());
//            SagaLifecycle.associateWith("receptionistId", event.staffId());
//
//            this.stateMachine = ExamWorkFlowProcessingStateMachine.PENDING_SEND_ITEM;
//
//            // ✅ KIỂM TRA LOẠI QUEUE ITEM NGAY TỪ EVENT - CHỈ XỬ LÝ EXAM_SERVICE
//            if (event.type() == QueueItemType.RECEPTION_PAYMENT) {
//                log.info(
//                        "[ExamWorkFlowProcessingSaga] Skipping RECEPTION_PAYMENT queue item: {}. This is handled by ReceptionPaymentProcessingSaga.",
//                        event.queueItemId());
//                // Kết thúc saga ngay lập tức cho mục thanh toán
//                SagaLifecycle.end();
//                return;
//            }
//
//            GetQueueItemResponseByIdQuery query = new GetQueueItemResponseByIdQuery(event.queueItemId());
//
//            Optional<QueueItemResponse> queueItemResponseOpt = queryGateway.query(query,
//                    ResponseTypes.optionalInstanceOf(QueueItemResponse.class))
//                    .join();
//
//            // bổ sung kiểm tra QueueItemResponse ở đây
//            QueueItemResponse queueItem = require(queueItemResponseOpt, "Phiếu chờ của bệnh nhân");
//
//            // bổ sung kiểm tra Optional<ServiceRepDto> và
//            // Optional<MedicalFormWithExamDetailsDto> ở
//            // đây
//            require(queueItem.medicalForm(), "Phiếu khám của bệnh nhân");
//            require(queueItem.requestedService(), "Dịch vụ yêu cầu");
//
//            wSNotifier.sendToUser(event.staffId(), queueItem);
//            wSNotifier.broadcast(event.queueId(), getQueueSize(event.queueId()));
//
//            this.stateMachine = ExamWorkFlowProcessingStateMachine.PENDING_CREATE_RESULT;
//
//        } catch (Exception e) {
//            log.error("[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] EXCEPTION occurred: {}", e.getClass()
//                    .getSimpleName());
//            log.error("[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Exception message: {}", e.getMessage());
//            log.error("[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Full exception: ", e);
//            handleException(e);
//        }
//    }
//
//    private <T> T require(Optional<T> opt, String message) {
//        return opt.orElseThrow(() -> new ResourceNotFoundException(message));
//    }
//
//    @SagaEventHandler(associationProperty = "doctorId")
//    public void handle(ResultAddedEvent event) {
//        this.stateMachine = ExamWorkFlowProcessingStateMachine.RESULT_CREATED;
//        CompleteQueueItemCommand cmd = new CompleteQueueItemCommand(queueItemId, staffId);
//        commandGateway.send(cmd)
//                .whenComplete((result, throwable) -> {
//                    if (throwable != null) {
//                        Throwable actual = throwable;
//
//                        // Bóc CompletionException
//                        if (actual instanceof CompletionException && actual.getCause() != null) {
//                            actual = actual.getCause();
//                        }
//                        // Bóc CommandExecutionException
//                        if (actual instanceof CommandExecutionException && actual.getCause() != null) {
//                            actual = actual.getCause();
//                        }
//
//                        if (actual instanceof IllegalStateException) {
//                            // TODO: ...
//                            return;
//                        }
//                    }
//                });
//    }
//
//    // @SagaEventHandler(associationProperty = "doctorId")
//    // public void handle(ResultSignedEvent event) {
//    // this.stateMachine = ExamWorkFlowProcessingStateMachine.RESULT_SIGNED;
//    //
//    // }
//
//    @EndSaga
//    @SagaEventHandler(associationProperty = "queueItemId")
//    public void handle(QueueItemCompletedEvent event) {
//        this.stateMachine = ExamWorkFlowProcessingStateMachine.COMPLETED;
//    }
//
//    private Long getQueueSize(String queueId) {
//        log.debug("[ExamWorkFlowProcessingSaga.getQueueSize] START: Getting queue size for queueId={}", queueId);
//        Long size = queryGateway.query(new GetQueueSizeQuery(queueId), ResponseTypes.instanceOf(Long.class))
//                .join();
//        log.debug("[ExamWorkFlowProcessingSaga.getQueueSize] SUCCESS: Queue size is {} for queueId={}", size, queueId);
//        return size;
//    }
//
//    private void handleException(Throwable throwable) {
//        log.warn("[ExamWorkFlowProcessingSaga.handleException] Exception occurred in state {}: {}", this.stateMachine,
//                throwable.getMessage());
//        log.warn("[ExamWorkFlowProcessingSaga.handleException] Exception class: {}", throwable.getClass()
//                .getName());
//        log.warn("[ExamWorkFlowProcessingSaga.handleException] Full stack trace: ", throwable);
//
//        if (this.staffId != null) {
//            log.debug("[ExamWorkFlowProcessingSaga.handleException] Notifying staff {} about error: {}", this.staffId,
//                    throwable.getMessage());
//            wSNotifier.notifyErrorToUser(this.staffId, throwable.getMessage());
//        } else {
//            log.warn("[ExamWorkFlowProcessingSaga.handleException] Cannot notify staff - staffId is null");
//        }
//
//        log.warn("[ExamWorkFlowProcessingSaga.handleException] Current saga state: {}", this.stateMachine);
//        // TODO: handle ExamWorkFlowProcessingStateMachine.TAKE_ITEM_REQUEST_RECEIVED;
//        // => end luôn
//        // TODO: handle ExamWorkFlowProcessingStateMachine.PENDING_SEND_ITEM => retry/
//        // compensation transaction
//
//        log.info("[ExamWorkFlowProcessingSaga.handleException] Ending saga");
//        SagaLifecycle.end();
//    }
//
//}
