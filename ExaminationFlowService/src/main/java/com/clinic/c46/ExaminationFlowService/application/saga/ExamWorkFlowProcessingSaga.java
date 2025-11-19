package com.clinic.c46.ExaminationFlowService.application.saga;


import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.medicalPackage.GetServiceByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDto;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetItemIdOfTopQueueQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetMedicalFormDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemDetailsByIdQuery;
import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.command.TakeNextItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.TakeNextItemRequestedEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.websocket.dto.QueueItemResponse;
import lombok.NoArgsConstructor;
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
public class ExamWorkFlowProcessingSaga {
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient WebSocketNotifier wSNotifier;

    private String staffId;
    private String queueId;
    private String queueItemId;

    private ExamWorkFlowProcessingStateMachine stateMachine;

    @StartSaga
    @SagaEventHandler(associationProperty = "queueId")
    public void on(TakeNextItemRequestedEvent event) {
        this.stateMachine = ExamWorkFlowProcessingStateMachine.TAKE_ITEM_REQUEST_RECEIVED;
        this.queueId = event.queueId();
        this.staffId = event.staffId();

        queryGateway.query(new GetItemIdOfTopQueueQuery(this.queueId), ResponseTypes.optionalInstanceOf(String.class))
                .join()
                .ifPresentOrElse(itemId -> {
                            try {
                                this.queueItemId = itemId;
                                SagaLifecycle.associateWith("queueItemId", itemId);
                                commandGateway.sendAndWait(new TakeNextItemCommand(itemId, this.staffId));
                                this.stateMachine = ExamWorkFlowProcessingStateMachine.PENDING_DEQUEUE;
                            } catch (Exception e) {
                                handleException(e);
                            }
                        },
                        // TODO: Không có item, kết thúc Saga
                        SagaLifecycle::end);

    }

    @SagaEventHandler(associationProperty = "queueItemId")
    public void on(QueueItemTakenEvent event) {
        try {
            this.stateMachine = ExamWorkFlowProcessingStateMachine.PENDING_SEND_ITEM;
            QueueItemDto queueItemDto = this.getQueueItem(event.queueItemId());
            MedicalFormDetailsDto medicalFormDetailsDto = this.getMedicalFormDetails(queueItemDto.medicalFormId());
            ServiceRepDto serviceRepDto = getService(queueItemDto.serviceId());
            QueueItemResponse queueItem = QueueItemResponse.builder()
                    .queueItem(queueItemDto)
                    .medicalForm(medicalFormDetailsDto)
                    .requestedService(serviceRepDto)
                    .build();
            wSNotifier.sendToUser(event.staffId(), queueItem);

            wSNotifier.broadcast(event.queueId(), getQueueSize(event.queueId()));

            this.stateMachine = ExamWorkFlowProcessingStateMachine.ITEM_SENT;
        } catch (Exception e) {
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
            log.debug("Phiếu khám bệnh không tồn tại: {}", medicalFormId);
            throw new ResourceNotFoundException("Phiếu khám bệnh");
        }

        MedicalFormDetailsDto medicalFormDetailsDto = medicalFormDetailsOptional.get();
        if (medicalFormDetailsDto.patient()
                .isEmpty()) {
            log.debug("Bệnh nhân không tồn tại!: mã phiếu khám: {}", medicalFormId);
            throw new ResourceNotFoundException("Bệnh nhân");
        }

        if (medicalFormDetailsDto.examination()
                .isEmpty()) {
            log.debug("Hồ sơ của bệnh nhân không tồn tại! mã phiếu khám {}", medicalFormId);
            throw new ResourceNotFoundException("Hồ sơ của bệnh nhân");
        }


        return medicalFormDetailsDto;
    }

    private QueueItemDto getQueueItem(String queueItemId) {
        GetQueueItemDetailsByIdQuery getQueueItemDetailsByIdQuery = new GetQueueItemDetailsByIdQuery(queueItemId);
        Optional<QueueItemDto> queueItemDto = queryGateway.query(getQueueItemDetailsByIdQuery,
                        ResponseTypes.optionalInstanceOf(QueueItemDto.class))
                .join();

        if (queueItemDto.isEmpty()) {
            log.debug("Queue Item not found!: {}", queueItemId);
            throw new ResourceNotFoundException("Phiếu chờ");
        }
        return queueItemDto.get();
    }

    private ServiceRepDto getService(String serviceId) {
        GetServiceByIdQuery getServiceByIdQuery = new GetServiceByIdQuery(serviceId);
        Optional<ServiceRepDto> serviceRepDto = queryGateway.query(getServiceByIdQuery,
                        ResponseTypes.optionalInstanceOf(ServiceRepDto.class))
                .join();
        if (serviceRepDto.isEmpty()) {
            throw new ResourceNotFoundException("Service not found: " + serviceId);
        }
        return serviceRepDto.get();
    }

    private Long getQueueSize(String queueId) {
        return queryGateway.query(new GetQueueSizeQuery(queueId), ResponseTypes.instanceOf(Long.class))
                .join();
    }

    private void handleException(Throwable throwable) {
        wSNotifier.notifyErrorToUser(this.staffId, throwable.getMessage());

        // TODO: handle ExamWorkFlowProcessingStateMachine.TAKE_ITEM_REQUEST_RECEIVED; => end luôn
        // TODO: handle ExamWorkFlowProcessingStateMachine.PENDING_SEND_ITEM => retry/ compensation transaction

        SagaLifecycle.end();
    }


}
