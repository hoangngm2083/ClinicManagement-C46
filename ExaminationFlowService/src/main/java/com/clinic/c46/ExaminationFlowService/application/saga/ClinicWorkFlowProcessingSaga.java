package com.clinic.c46.ExaminationFlowService.application.saga;

import com.clinic.c46.CommonService.command.examination.CreateExaminationCommand;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllServicesOfPackagesQuery;
import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.command.CreateQueueItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.MedicalFormCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemProcessedEvent;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.*;

@Saga
@Slf4j
@NoArgsConstructor
public class ClinicWorkFlowProcessingSaga {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private transient WebSocketNotifier wSNotifier;
    @Autowired
    private transient DeadlineManager deadlineManager;

    private String medicalFormId;
    private String patientId;
    private String invoiceId;
    private String examinationId;
    private String queueItemProcessingId;

    // TRONG PHẦN KHAI BÁO BIẾN
    private PriorityQueue<ServiceRepDto> requestServiceSorted = new PriorityQueue<>(
            (a, b) -> a.processingPriority() - b.processingPriority());

    // KHỞI TẠO SET NÀY!
    private Set<String> completedServices = new HashSet<>();

    private ClinicWorkFlowProcessingStateMachine stateMachine;

    @StartSaga
    @SagaEventHandler(associationProperty = "medicalFormId")
    public void on(MedicalFormCreatedEvent event) {
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.MEDICAL_FORM_CREATED;
        debugTrace(event);

        this.medicalFormId = event.medicalFormId();
        this.patientId = event.patientId();
        this.invoiceId = event.invoiceId();
        this.examinationId = event.examinationId();
        // Get all service requested
        List<ServiceRepDto> services;
        try {
            services = extractServicesOfAllPackages(event.packageIds());
            this.requestServiceSorted.addAll(services);
        } catch (ResourceNotFoundException e) {
            // This is a business error, not a technical error
            log.warn("[ExamWorkFlowProcessingSaga] No services found for package IDs {}. Ending Saga.",
                    event.packageIds());
            SagaLifecycle.end();
            return;
        }
        this.requestServiceSorted.addAll(services);

        // Send command add Create Examination: patientId, examinationId
        SagaLifecycle.associateWith("examinationId", this.examinationId);
        CreateExaminationCommand command = CreateExaminationCommand.builder()
                .patientId(this.patientId)
                .examinationId(this.examinationId)
                .build();

        this.sendCmd(command);

        this.stateMachine = ClinicWorkFlowProcessingStateMachine.PENDING_CREATE_EXAMINATION;
        debugTrace(command);
    }

    public List<ServiceRepDto> extractServicesOfAllPackages(Set<String> packageIds) throws ResourceNotFoundException {
        List<ServiceRepDto> services = queryGateway.query(GetAllServicesOfPackagesQuery.builder()
                        .packageIds(packageIds)
                        .build(), ResponseTypes.multipleInstancesOf(ServiceRepDto.class))
                .join();
        if (services.isEmpty()) {
            throw new ResourceNotFoundException("No package's services found for IDs: " + packageIds);
        }
        return services;
    }

    @SagaEventHandler(associationProperty = "examinationId")
    public void handle(ExaminationCreatedEvent event) {
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.EXAMINATION_CREATED;
        debugTrace(event);
        // Send command add Medical Form to Queue
        processNextServiceOrPayment();

    }

    @SagaEventHandler(associationProperty = "queueItemId")
    private void handle(QueueItemCreatedEvent event) {
        if (this.stateMachine == ClinicWorkFlowProcessingStateMachine.QUEUE_ITEM_CREATED) {
            return;
        }
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.QUEUE_ITEM_CREATED;
        debugTrace(event);
        clearCreateQueueItemDeadline();
        Long qSize = queryGateway.query(new GetQueueSizeQuery(event.queueId()), ResponseTypes.instanceOf(Long.class))
                .join();
        wSNotifier.broadcast(event.queueId(), qSize);
    }

    @SagaEventHandler(associationProperty = "queueItemId")
    private void handle(QueueItemProcessedEvent event) {
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.QUEUE_ITEM_PROCESSED;
        debugTrace(event);
        if (this.completedServices.contains(event.serviceId())) {
            log.warn("Duplicate event: {}", event.serviceId());
            return;
        }

        // Dịch vụ vửa xử lý xong là PAYMENT_REQUEST -> end
        if (event.serviceId()
                .equals("PAYMENT_REQUEST")) {
            log.info("[ExamWorkFlowProcessingSaga] All services completed for medical form: {}", this.medicalFormId);
            // TODO: có thể yêu cầu build pdf và gửi hồ sơ về email bệnh nhân
            return;
        }

        this.completedServices.add(this.requestServiceSorted.poll()
                .serviceId());

        // Nếu không phải
        processNextServiceOrPayment();
    }

    private void processNextServiceOrPayment() {
        // Dịch vụ vừa xử lý xong không phải là PAYMENT_REQUEST-> tiếp tục
        if (this.requestServiceSorted.isEmpty()) // Nếu hàng đợi rỗng -> đầy vào hàng đợi thanh toán.
        {
            // TODO:  đầy vào hàng đợi thanh toán.
            ServiceRepDto paymentService = ServiceRepDto.builder()
                    .serviceId("PAYMENT_REQUEST")
                    .name("PAYMENT_REQUEST")
                    .departmentId("receptionist")
                    .processingPriority(9999)
                    .build();
            this.requestServiceSorted.add(paymentService);
        }

        ServiceRepDto serviceToProcess = this.requestServiceSorted.peek();

        this.queueItemProcessingId = UUID.randomUUID()
                .toString();
        SagaLifecycle.associateWith("queueItemId", this.queueItemProcessingId);

        CreateQueueItemCommand cmd = CreateQueueItemCommand.builder()
                .queueItemId(queueItemProcessingId)
                .serviceId(serviceToProcess.serviceId())
                .medicalFormId(this.medicalFormId)
                .queueId(serviceToProcess.departmentId())
                .build();

        this.stateMachine = ClinicWorkFlowProcessingStateMachine.PENDING_CREATE_QUEUE_ITEM;
        this.sendCmd(cmd);
        setCreateQueueItemDeadline(cmd);
    }


    private void sendCmd(Object cmd) {
        commandGateway.send(cmd)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("[exam-flow.ExamWorkFlowProcessingSaga] Error: {}", throwable.getMessage());
                        log.error("[exam-flow.ExamWorkFlowProcessingSaga] Cause: {}", throwable.getCause()
                                .getMessage());
                    }
                });
    }


    private void debugTrace(Object obj) {
        log.debug("[ExamWorkFlowProcessingSaga] State Machine: {}", this.stateMachine);
        log.debug("[ExamWorkFlowProcessingSaga] Object: {}", obj.toString());
    }

    @DeadlineHandler(deadlineName = "queue-item-timeout")
    public void onTimeout(CreateQueueItemCommand cmd) {
        log.warn("[ExamWorkFlowProcessingSaga] Deadline triggered for QueueItem: {}. Service: {}. RETRYING...",
                cmd.queueItemId(), cmd.serviceId());
        this.sendCmd(cmd);
        setCreateQueueItemDeadline(cmd);
    }

    private void setCreateQueueItemDeadline(Object cmd) {
        deadlineManager.schedule(Duration.ofMinutes(5), "queue-item-timeout", cmd);
    }

    private void clearCreateQueueItemDeadline() {
        deadlineManager.cancelAllWithinScope("queue-item-timeout");
    }

}

