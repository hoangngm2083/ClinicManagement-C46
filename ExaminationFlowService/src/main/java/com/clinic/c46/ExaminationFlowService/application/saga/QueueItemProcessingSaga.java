package com.clinic.c46.ExaminationFlowService.application.saga;

import com.clinic.c46.CommonService.event.examination.ResultAddedEvent;
import com.clinic.c46.CommonService.event.payment.TransactionCompletedEvent;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormWithExamDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormWithInvoiceDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemResponse;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemResponseByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
import com.clinic.c46.ExaminationFlowService.domain.command.CompleteQueueItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCompletedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Unified Saga for managing queue item lifecycle.
 * <p>
 * Handles both:
 * 1. EXAM_SERVICE: Laboratory/examination services processing
 * 2. RECEPTION_PAYMENT: Payment processing at reception desk
 * <p>
 * Type-based routing:
 * - On EXAM_SERVICE: Waits for ResultAddedEvent from examination
 * - On RECEPTION_PAYMENT: Waits for TransactionCompletedEvent from payment
 * service
 */
@Saga
@NoArgsConstructor
@Slf4j
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueItemProcessingSaga {

    @Autowired
    @JsonIgnore
    private transient QueryGateway queryGateway;

    @Autowired
    @JsonIgnore
    private transient CommandGateway commandGateway;

    @Autowired
    @JsonIgnore
    private transient WebSocketNotifier wSNotifier;

    // Saga state variables
    private String staffId;
    private String queueId;
    private String queueItemId;
    private QueueItemType queueItemType;
    private String invoiceId;

    private QueueItemProcessingStateMachine stateMachine;

    /**
     * Starts saga when QueueItemTakenEvent is triggered.
     * Routes to appropriate flow based on queue item type.
     */
    @StartSaga
    @SagaEventHandler(associationProperty = "queueItemId")
    public void on(QueueItemTakenEvent event) {
        try {
            log.info(
                    "[QueueItemProcessingSaga] START: Received QueueItemTakenEvent. "
                            + "QueueItemId: {}, StaffId: {}, Type: {}",
                    event.queueItemId(), event.staffId(), event.type());

            // Store common state
            this.queueId = event.queueId();
            this.staffId = event.staffId();
            this.queueItemId = event.queueItemId();
            this.queueItemType = event.type();

            // Setup saga associations
            SagaLifecycle.associateWith("staffId", event.staffId());
            SagaLifecycle.associateWith("queueItemId", event.queueItemId());

            this.stateMachine = QueueItemProcessingStateMachine.PENDING_SEND_ITEM;

            // Route to appropriate handler based on type
            if (event.type() == QueueItemType.EXAM_SERVICE) {
                handleExamServiceFlow(event);
            } else if (event.type() == QueueItemType.RECEPTION_PAYMENT) {
                handleReceptionPaymentFlow(event);
            } else {
                log.warn("[QueueItemProcessingSaga] Unknown queue item type: {}", event.type());
                SagaLifecycle.end();
            }

        } catch (Exception e) {
            log.error("[QueueItemProcessingSaga.on(QueueItemTakenEvent)] EXCEPTION: {}", e.getClass()
                    .getSimpleName());
            log.error("[QueueItemProcessingSaga.on(QueueItemTakenEvent)] Message: {}", e.getMessage());
            log.error("[QueueItemProcessingSaga.on(QueueItemTakenEvent)] Full stack: ", e);
            handleException(e);
        }
    }

    /**
     * Handles EXAM_SERVICE flow processing
     */
    private void handleExamServiceFlow(QueueItemTakenEvent event) {
        log.info("[QueueItemProcessingSaga] Processing EXAM_SERVICE flow for QueueItemId: {}", event.queueItemId());

        try {
            // Query queue item details
            GetQueueItemResponseByIdQuery query = new GetQueueItemResponseByIdQuery(event.queueItemId());
            Optional<QueueItemResponse> queueItemResponseOpt = queryGateway.query(query,
                    ResponseTypes.optionalInstanceOf(QueueItemResponse.class))
                    .join();

            QueueItemResponse queueItem = require(queueItemResponseOpt, "Phiếu chờ của bệnh nhân");

            // Validate exam-specific fields
            require(queueItem.medicalForm(), "Phiếu khám của bệnh nhân");
            require(queueItem.requestedService(), "Dịch vụ yêu cầu");

            // Notify staff
            wSNotifier.sendToUser(event.staffId(), queueItem);
            wSNotifier.broadcast(event.queueId(), getQueueSize(event.queueId()));

            // Setup associations for exam flow
            if (queueItem.medicalForm()
                    .get() instanceof MedicalFormWithExamDetailsDto examForm) {
                if (examForm.examination()
                        .isPresent()) {
                    String examId = examForm.examination()
                            .get()
                            .id();
                    SagaLifecycle.associateWith("examinationId", examId);
                    log.info("[QueueItemProcessingSaga] Associated with examinationId: {}", examId);
                } else {
                    throw new ResourceNotFoundException("Thông tin lượt khám trong phiếu khám");
                }
            } else {
                log.warn(
                        "[QueueItemProcessingSaga] Medical form is not of type MedicalFormWithExamDetailsDto for EXAM_SERVICE");
                // Fallback (though this indicates a logic error elsewhere if it happens)
                SagaLifecycle.associateWith("examinationId", queueItem.medicalForm()
                        .get()
                        .id());
            }

            this.stateMachine = QueueItemProcessingStateMachine.PENDING_CREATE_RESULT;

            log.info(
                    "[QueueItemProcessingSaga] EXAM_SERVICE flow ready. "
                            + "Waiting for ResultAddedEvent. QueueItemId: {}",
                    event.queueItemId());

        } catch (Exception e) {
            log.error("[QueueItemProcessingSaga.handleExamServiceFlow] Error: {}", e.getClass()
                    .getSimpleName());
            throw e;
        }
    }

    /**
     * Handles RECEPTION_PAYMENT flow processing
     */
    private void handleReceptionPaymentFlow(QueueItemTakenEvent event) {
        log.info("[QueueItemProcessingSaga] Processing RECEPTION_PAYMENT flow for QueueItemId: {}",
                event.queueItemId());

        try {
            // Query queue item details
            GetQueueItemResponseByIdQuery query = new GetQueueItemResponseByIdQuery(event.queueItemId());
            Optional<QueueItemResponse> queueItemResponseOpt = queryGateway.query(query,
                    ResponseTypes.optionalInstanceOf(QueueItemResponse.class))
                    .join();

            QueueItemResponse queueItem = require(queueItemResponseOpt, "Phiếu chờ thanh toán không tồn tại");

            require(queueItem.medicalForm(), "Phiếu khám của bệnh nhân");
            require(queueItem.requestedService(), "Dịch vụ yêu cầu");

            // Setup associations for payment flow
            if (queueItem.medicalForm()
                    .get() instanceof MedicalFormWithInvoiceDetailsDto invoiceForm) {
                if (invoiceForm.invoice()
                        .isPresent()) {
                    this.invoiceId = invoiceForm.invoice()
                            .get()
                            .invoiceId();
                    SagaLifecycle.associateWith("invoiceId", this.invoiceId);
                    log.info("[QueueItemProcessingSaga] Associated with invoiceId: {}", this.invoiceId);
                } else {
                    throw new ResourceNotFoundException("Thông tin hóa đơn không tồn tại trong phiếu khám");
                }
            } else {
                log.warn(
                        "[QueueItemProcessingSaga] Medical form is not of type MedicalFormWithInvoiceDetailsDto for RECEPTION_PAYMENT");
                // Fallback
                this.invoiceId = queueItem.medicalForm()
                        .get()
                        .id();
                SagaLifecycle.associateWith("invoiceId", this.invoiceId);
            }

            // Notify staff
            wSNotifier.sendToUser(event.staffId(), queueItem);
            wSNotifier.broadcast(event.queueId(), getQueueSize(event.queueId()));

            this.stateMachine = QueueItemProcessingStateMachine.WAITING_FOR_PAYMENT;

            log.info(
                    "[QueueItemProcessingSaga] RECEPTION_PAYMENT flow ready. "
                            + "Waiting for TransactionCompletedEvent. InvoiceId: {}",
                    this.invoiceId);

        } catch (Exception e) {
            log.error("[QueueItemProcessingSaga.handleReceptionPaymentFlow] Error: {}", e.getClass()
                    .getSimpleName());
            throw e;
        }
    }

    /**
     * Handles ResultAddedEvent (for EXAM_SERVICE flow)
     */
    @SagaEventHandler(associationProperty = "examinationId")
    public void handle(ResultAddedEvent event) {
        try {
            log.info("[QueueItemProcessingSaga] Received ResultAddedEvent. " + "QueueItemId: {}, DoctorId: {}",
                    this.queueItemId, event.doctorId());

            // Only process if this is an EXAM_SERVICE item
            if (this.queueItemType != QueueItemType.EXAM_SERVICE) {
                log.warn("[QueueItemProcessingSaga] Ignoring ResultAddedEvent for non-EXAM_SERVICE item. " + "Type: {}",
                        this.queueItemType);
                return;
            }

            this.stateMachine = QueueItemProcessingStateMachine.RESULT_CREATED;

            // Complete the queue item
            CompleteQueueItemCommand cmd = new CompleteQueueItemCommand(queueItemId, staffId);
            commandGateway.send(cmd)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            Throwable actual = throwable;

                            // Unwrap CompletionException
                            if (actual instanceof CompletionException && actual.getCause() != null) {
                                actual = actual.getCause();
                            }
                            // Unwrap CommandExecutionException
                            if (actual instanceof CommandExecutionException && actual.getCause() != null) {
                                actual = actual.getCause();
                            }

                            if (actual instanceof IllegalStateException) {
                                log.warn("[QueueItemProcessingSaga] IllegalStateException: {}", actual.getMessage());
                                return;
                            }

                            log.error("[QueueItemProcessingSaga] Error completing queue item: {}", actual.getMessage());
                        } else {
                            log.info(
                                    "[QueueItemProcessingSaga] Queue item completed successfully. " + "QueueItemId: {}",
                                    queueItemId);
                        }
                    });

        } catch (Exception e) {
            log.error("[QueueItemProcessingSaga.handle(ResultAddedEvent)] EXCEPTION: {}", e.getMessage(), e);
            handleException(e);
        }
    }

    /**
     * Handles TransactionCompletedEvent (for RECEPTION_PAYMENT flow)
     */
    @SagaEventHandler(associationProperty = "invoiceId")
    public void handle(TransactionCompletedEvent event) {
        try {
            log.info(
                    "[QueueItemProcessingSaga] Received TransactionCompletedEvent. "
                            + "InvoiceId: {}, TransactionId: {}",
                    event.invoiceId(), event.transactionId());

            // Only process if this is a RECEPTION_PAYMENT item
            if (this.queueItemType != QueueItemType.RECEPTION_PAYMENT) {
                log.warn(
                        "[QueueItemProcessingSaga] Ignoring TransactionCompletedEvent for non-RECEPTION_PAYMENT item. "
                                + "Type: {}",
                        this.queueItemType);
                return;
            }

            this.stateMachine = QueueItemProcessingStateMachine.PAYMENT_COMPLETED;

            // Complete the queue item
            CompleteQueueItemCommand cmd = new CompleteQueueItemCommand(queueItemId, staffId);
            commandGateway.send(cmd)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("[QueueItemProcessingSaga] Error completing queue item after payment: {}",
                                    throwable.getMessage());
                        } else {
                            log.info(
                                    "[QueueItemProcessingSaga] Queue item completed after payment. "
                                            + "QueueItemId: {}",
                                    queueItemId);
                        }
                    });

        } catch (Exception e) {
            log.error("[QueueItemProcessingSaga.handle(TransactionCompletedEvent)] EXCEPTION: {}", e.getMessage(), e);
            handleException(e);
        }
    }

    /**
     * Ends saga when QueueItemCompletedEvent is triggered
     */
    @EndSaga
    @SagaEventHandler(associationProperty = "queueItemId")
    public void handle(QueueItemCompletedEvent event) {
        log.info("[QueueItemProcessingSaga] Received QueueItemCompletedEvent. QueueItemId: {}", event.queueItemId());
        this.stateMachine = QueueItemProcessingStateMachine.COMPLETED;
    }

    private <T> T require(Optional<T> opt, String message) {
        return opt.orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private Long getQueueSize(String queueId) {
        try {
            log.debug("[QueueItemProcessingSaga] Getting queue size for queueId={}", queueId);
            Long size = queryGateway.query(new GetQueueSizeQuery(queueId), ResponseTypes.instanceOf(Long.class))
                    .join();
            log.debug("[QueueItemProcessingSaga] Queue size: {} for queueId={}", size, queueId);
            return size;
        } catch (Exception e) {
            log.warn("[QueueItemProcessingSaga] Error getting queue size: {}", e.getMessage());
            return 0L;
        }
    }

    private void handleException(Throwable throwable) {
        log.warn("[QueueItemProcessingSaga] Exception in state {}: {}", this.stateMachine, throwable.getMessage());
        log.warn("[QueueItemProcessingSaga] Exception class: {}", throwable.getClass()
                .getName());

        if (this.staffId != null) {
            try {
                String errorMessage = String.format("Lỗi xử lý phiếu: %s", throwable.getMessage());
                wSNotifier.notifyErrorToUser(this.staffId, errorMessage);
            } catch (Exception e) {
                log.error("[QueueItemProcessingSaga] Error notifying user: {}", e.getMessage());
            }
        }

        log.info("[QueueItemProcessingSaga] Ending saga due to exception");
        SagaLifecycle.end();
    }
}
