package com.clinic.c46.ExaminationFlowService.application.saga;


import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetServiceByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDto;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetItemIdOfTopQueueQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetMedicalFormDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.command.TakeNextItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.TakeNextItemRequestedEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.QueueItemResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Saga
@NoArgsConstructor
@Slf4j
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamWorkFlowProcessingSaga {
    @Autowired
    @JsonIgnore
    private transient QueryGateway queryGateway;
    @Autowired
    @JsonIgnore
    private transient CommandGateway commandGateway;
    @Autowired
    @JsonIgnore
    private transient WebSocketNotifier wSNotifier;

    private String staffId;
    private String queueId;
    private String queueItemId;

    private ExamWorkFlowProcessingStateMachine stateMachine;

    @StartSaga
    @SagaEventHandler(associationProperty = "queueId")
    public void on(TakeNextItemRequestedEvent event) {
        log.info("[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] SAGA STARTED: New saga instance created");
        log.info("[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Event received - queueId={}, staffId={}",
                event.queueId(), event.staffId());

        this.stateMachine = ExamWorkFlowProcessingStateMachine.TAKE_ITEM_REQUEST_RECEIVED;
        this.queueId = event.queueId();
        this.staffId = event.staffId();
        log.info("[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Saga state initialized");

        log.debug(
                "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Querying for top item in queue: queueId={}",
                this.queueId);
        queryGateway.query(new GetItemIdOfTopQueueQuery(this.queueId), ResponseTypes.optionalInstanceOf(String.class))
                .join()
                .ifPresentOrElse(itemId -> {
                            try {
                                log.info(
                                        "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Top item found in queue: queueItemId={}",
                                        itemId);
                                this.queueItemId = itemId;
                                SagaLifecycle.associateWith("queueItemId", itemId);
                                log.debug(
                                        "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Associated saga with queueItemId={}",
                                        itemId);

                                log.debug(
                                        "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Sending TakeNextItemCommand: itemId={}, staffId={}",
                                        itemId, this.staffId);
                                commandGateway.sendAndWait(new TakeNextItemCommand(itemId, this.staffId));
                                log.info(
                                        "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] TakeNextItemCommand executed successfully");

                                this.stateMachine = ExamWorkFlowProcessingStateMachine.PENDING_DEQUEUE;
                                log.info(
                                        "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] State changed to PENDING_DEQUEUE, waiting for QueueItemTakenEvent");
                            } catch (Exception e) {
                                log.error(
                                        "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] Exception in itemPresent block: {}",
                                        e.getMessage(), e);
                                handleException(e);
                            }
                        },
                        // TODO: Không có item, kết thúc Saga
                        () -> {
                            log.warn(
                                    "[ExamWorkFlowProcessingSaga.on(TakeNextItemRequestedEvent)] No items in queue - queueId={}, ending saga",
                                    this.queueId);
                            SagaLifecycle.end();
                        });

    }

    @SagaEventHandler(associationProperty = "queueItemId")
    public void on(QueueItemTakenEvent event) {

        try {
            this.stateMachine = ExamWorkFlowProcessingStateMachine.PENDING_SEND_ITEM;

            QueueItemDto queueItemDto = this.getQueueItem(event.queueItemId());

            MedicalFormDetailsDto medicalFormDetailsDto = this.getMedicalFormDetails(queueItemDto.medicalFormId());

            ServiceRepDto serviceRepDto = getService(queueItemDto.serviceId());

            QueueItemResponse queueItem = QueueItemResponse.builder()
                    .queueItemId(event.queueItemId())
                    .medicalForm(medicalFormDetailsDto)
                    .requestedService(serviceRepDto)
                    .build();

            wSNotifier.sendToUser(event.staffId(), queueItem);

            wSNotifier.broadcast(event.queueId(), getQueueSize(event.queueId()));

            this.stateMachine = ExamWorkFlowProcessingStateMachine.ITEM_SENT;

        } catch (Exception e) {
            log.error("[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] EXCEPTION occurred: {}", e.getClass()
                    .getSimpleName());
            log.error("[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Exception message: {}", e.getMessage());
            log.error("[ExamWorkFlowProcessingSaga.on(QueueItemTakenEvent)] Full exception: ", e);
            handleException(e);
        }
    }

    private MedicalFormDetailsDto getMedicalFormDetails(String medicalFormId) {


        GetMedicalFormDetailsByIdQuery getMedicalFormDetailsByIdQuery = new GetMedicalFormDetailsByIdQuery(
                medicalFormId);


        CompletableFuture<Optional<MedicalFormDetailsDto>> medicalFormDetailsFutureResult = queryGateway.query(
                getMedicalFormDetailsByIdQuery, ResponseTypes.optionalInstanceOf(MedicalFormDetailsDto.class));

        Optional<MedicalFormDetailsDto> medicalFormDetailsOptional = medicalFormDetailsFutureResult.join();

        if (medicalFormDetailsOptional.isEmpty()) {
            log.warn(
                    "[ExamWorkFlowProcessingSaga.getMedicalFormDetails] ERROR: Medical form view is EMPTY from query result for medicalFormId={}",
                    medicalFormId);
            throw new ResourceNotFoundException("Phiếu khám bệnh");
        }

        MedicalFormDetailsDto medicalFormDetailsDto = medicalFormDetailsOptional.get();



        if (medicalFormDetailsDto.examination()
                .isEmpty()) {
            throw new ResourceNotFoundException("Hồ sơ của bệnh nhân");
        }
        return medicalFormDetailsDto;
    }

    private QueueItemDto getQueueItem(String queueItemId) {
        log.debug("[ExamWorkFlowProcessingSaga.getQueueItem] START: Retrieving queue item for queueItemId={}",
                queueItemId);
        GetQueueItemDetailsByIdQuery getQueueItemDetailsByIdQuery = new GetQueueItemDetailsByIdQuery(queueItemId);
        Optional<QueueItemDto> queueItemDto = queryGateway.query(getQueueItemDetailsByIdQuery,
                        ResponseTypes.optionalInstanceOf(QueueItemDto.class))
                .join();

        if (queueItemDto.isEmpty()) {
            log.warn("[ExamWorkFlowProcessingSaga.getQueueItem] ERROR: Queue Item not found for queueItemId={}",
                    queueItemId);
            throw new ResourceNotFoundException("Phiếu chờ");
        }
        log.info(
                "[ExamWorkFlowProcessingSaga.getQueueItem] SUCCESS: Queue item retrieved - serviceId={}, medicalFormId={}",
                queueItemDto.get()
                        .serviceId(), queueItemDto.get()
                        .medicalFormId());
        return queueItemDto.get();
    }

    private ServiceRepDto getService(String serviceId) {
        log.debug("[ExamWorkFlowProcessingSaga.getService] START: Retrieving service details for serviceId={}",
                serviceId);
        GetServiceByIdQuery getServiceByIdQuery = new GetServiceByIdQuery(serviceId);
        Optional<ServiceRepDto> serviceRepDto = queryGateway.query(getServiceByIdQuery,
                        ResponseTypes.optionalInstanceOf(ServiceRepDto.class))
                .join();
        if (serviceRepDto.isEmpty()) {
            log.warn("[ExamWorkFlowProcessingSaga.getService] ERROR: Service not found for serviceId={}", serviceId);
            throw new ResourceNotFoundException("Service not found: " + serviceId);
        }
        log.info("[ExamWorkFlowProcessingSaga.getService] SUCCESS: Service retrieved - name={}", serviceRepDto.get()
                .name());
        return serviceRepDto.get();
    }

    private Long getQueueSize(String queueId) {
        log.debug("[ExamWorkFlowProcessingSaga.getQueueSize] START: Getting queue size for queueId={}", queueId);
        Long size = queryGateway.query(new GetQueueSizeQuery(queueId), ResponseTypes.instanceOf(Long.class))
                .join();
        log.debug("[ExamWorkFlowProcessingSaga.getQueueSize] SUCCESS: Queue size is {} for queueId={}", size, queueId);
        return size;
    }

    private void handleException(Throwable throwable) {
        log.warn("[ExamWorkFlowProcessingSaga.handleException] Exception occurred in state {}: {}", this.stateMachine,
                throwable.getMessage());
        log.warn("[ExamWorkFlowProcessingSaga.handleException] Exception class: {}", throwable.getClass()
                .getName());
        log.warn("[ExamWorkFlowProcessingSaga.handleException] Full stack trace: ", throwable);

        if (this.staffId != null) {
            log.debug("[ExamWorkFlowProcessingSaga.handleException] Notifying staff {} about error: {}", this.staffId,
                    throwable.getMessage());
            wSNotifier.notifyErrorToUser(this.staffId, throwable.getMessage());
        } else {
            log.warn("[ExamWorkFlowProcessingSaga.handleException] Cannot notify staff - staffId is null");
        }

        log.warn("[ExamWorkFlowProcessingSaga.handleException] Current saga state: {}", this.stateMachine);
        // TODO: handle ExamWorkFlowProcessingStateMachine.TAKE_ITEM_REQUEST_RECEIVED; => end luôn
        // TODO: handle ExamWorkFlowProcessingStateMachine.PENDING_SEND_ITEM => retry/ compensation transaction

        log.info("[ExamWorkFlowProcessingSaga.handleException] Ending saga");
        SagaLifecycle.end();
    }


}
