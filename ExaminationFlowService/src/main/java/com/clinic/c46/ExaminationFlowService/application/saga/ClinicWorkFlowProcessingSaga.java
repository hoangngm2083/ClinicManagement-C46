package com.clinic.c46.ExaminationFlowService.application.saga;

import com.clinic.c46.CommonService.command.examination.CreateExaminationCommand;
import com.clinic.c46.CommonService.command.payment.CreateInvoiceCommand;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.CommonService.event.payment.InvoiceCreatedEvent;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllServicesOfPackagesQuery;
import com.clinic.c46.ExaminationFlowService.application.service.websocket.WebSocketNotifier;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemAggregate;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType;
import com.clinic.c46.ExaminationFlowService.domain.command.CompleteMedicalFormCommand;
import com.clinic.c46.ExaminationFlowService.domain.command.CreateQueueItemCommand;
import com.clinic.c46.ExaminationFlowService.domain.event.MedicalFormCompletedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.MedicalFormCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCompletedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCreatedEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClinicWorkFlowProcessingSaga {
    @Autowired
    @JsonIgnore
    private transient CommandGateway commandGateway;
    @Autowired
    @JsonIgnore
    private transient QueryGateway queryGateway;
    @Autowired
    @JsonIgnore
    private transient WebSocketNotifier wSNotifier;
    @Autowired
    @JsonIgnore
    private transient DeadlineManager deadlineManager;

    private String medicalFormId;
    private String patientId;
    private String invoiceId;
    private String examinationId;
    private String queueItemProcessingId;
    // TRONG PHẦN KHAI BÁO BIẾN
    private PriorityQueue<ServiceRepDto> requestServiceSorted = new PriorityQueue<>();
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

        // Send command to create Invoice
        SagaLifecycle.associateWith("invoiceId", this.invoiceId);
        CreateInvoiceCommand invoiceCommand = CreateInvoiceCommand.builder()
                .invoiceId(this.invoiceId)
                .medicalFormId(this.medicalFormId)
                .build();

        this.sendCmd(invoiceCommand);
        log.info("[ClinicWorkFlowProcessingSaga] Sent CreateInvoiceCommand for invoiceId: {}, medicalFormId: {}",
                this.invoiceId, this.medicalFormId);
    }

    public List<ServiceRepDto> extractServicesOfAllPackages(Set<String> packageIds) throws ResourceNotFoundException {
        log.warn(" ++++++++++ Extracting services for package IDs: {}", packageIds);
        List<ServiceRepDto> services = queryGateway.query(GetAllServicesOfPackagesQuery.builder()
                .packageIds(packageIds)
                .build(), ResponseTypes.multipleInstancesOf(ServiceRepDto.class))
                .join();
        if (services.isEmpty()) {
            throw new ResourceNotFoundException("No package's services found for IDs: " + packageIds);
        }
        return services;
    }

    @SagaEventHandler(associationProperty = "invoiceId")
    public void handle(InvoiceCreatedEvent event) {
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.INVOICE_CREATED;
        // Send command add Create Examination: patientId, examinationId
        SagaLifecycle.associateWith("examinationId", this.examinationId);
        CreateExaminationCommand examCommand = CreateExaminationCommand.builder()
                .patientId(this.patientId)
                .examinationId(this.examinationId)
                .build();

        this.sendCmd(examCommand);

        this.stateMachine = ClinicWorkFlowProcessingStateMachine.PENDING_CREATE_EXAMINATION;
        debugTrace(examCommand);
    }

    @SagaEventHandler(associationProperty = "examinationId")
    public void handle(ExaminationCreatedEvent event) {

        log.warn("Received ExaminationCreatedEvent for examinationId: {}", event.examinationId());
        log.warn("Received ExaminationCreatedEvent for examinationId: {}", this.stateMachine);

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
    private void handle(QueueItemCompletedEvent event) {
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.QUEUE_ITEM_PROCESSED;
        debugTrace(event);
        if (this.completedServices.contains(event.serviceId())) {
            log.warn("Duplicate event: {}", event.serviceId());
            return;
        }

        // Dịch vụ vửa xử lý xong là PAYMENT_REQUEST -> complete medical form and end
        // saga
        if (event.serviceId()
                .equals("PAYMENT_REQUEST")) {
            log.info("[ClinicWorkFlowProcessingSaga] All services completed for medical form: {}", this.medicalFormId);

            // Send command to complete medical form
            CompleteMedicalFormCommand completeMedicalFormCommand = CompleteMedicalFormCommand.builder()
                    .medicalFormId(this.medicalFormId)
                    .build();

            this.sendCmd(completeMedicalFormCommand);
            log.info("[ClinicWorkFlowProcessingSaga] Sent CompleteMedicalFormCommand for medicalFormId: {}",
                    this.medicalFormId);

            // TODO: có thể yêu cầu build pdf và gửi hồ sơ về email bệnh nhân
            return;
        }

        ServiceRepDto serviceRepDto = this.requestServiceSorted.poll();

        log.warn("Processed service: {}", serviceRepDto.serviceId());

        this.completedServices.add(serviceRepDto.serviceId());

        processNextServiceOrPayment();
    }

    private void processNextServiceOrPayment() {
        // Dịch vụ vừa xử lý xong không phải là PAYMENT_REQUEST-> tiếp tục
        if (this.requestServiceSorted.isEmpty()) // Nếu hàng đợi rỗng -> đầy vào hàng đợi thanh toán.
        {
            // TODO: đầy vào hàng đợi thanh toán.
            ServiceRepDto paymentService = ServiceRepDto.builder()
                    .serviceId("PAYMENT_REQUEST")
                    .name("PAYMENT_REQUEST")
                    .departmentId(QueueItemAggregate.RECEPTION_QUEUE_ID)
                    .processingPriority(9999)
                    .build();
            this.requestServiceSorted.add(paymentService);
        }

        ServiceRepDto serviceToProcess = this.requestServiceSorted.peek();
        log.warn("Next service: {}", serviceToProcess.serviceId());
        this.queueItemProcessingId = UUID.randomUUID()
                .toString();
        SagaLifecycle.associateWith("queueItemId", this.queueItemProcessingId);

        // Xác định type dựa trên serviceId
        QueueItemType queueItemType = determineQueueItemType(serviceToProcess.serviceId());

        CreateQueueItemCommand cmd = CreateQueueItemCommand.builder()
                .queueItemId(queueItemProcessingId)
                .medicalFormId(this.medicalFormId)
                .serviceId(serviceToProcess.serviceId())
                .queueId(serviceToProcess.departmentId())
                .type(queueItemType)
                .build();

        this.stateMachine = ClinicWorkFlowProcessingStateMachine.PENDING_CREATE_QUEUE_ITEM;
        this.sendCmd(cmd);
        setCreateQueueItemDeadline(cmd);
    }

    /**
     * Xác định loại hàng đợi dựa trên serviceId
     */
    private QueueItemType determineQueueItemType(String serviceId) {
        if ("PAYMENT_REQUEST".equals(serviceId)) {
            return QueueItemType.RECEPTION_PAYMENT;
        }
        // Mặc định là EXAM_SERVICE cho các dịch vụ khám/cận lâm sàng
        return QueueItemType.EXAM_SERVICE;
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

    @SagaEventHandler(associationProperty = "medicalFormId")
    public void handle(MedicalFormCompletedEvent event) {
        log.info("[ClinicWorkFlowProcessingSaga] Medical form completed: {}. Ending saga.", event.medicalFormId());
        this.stateMachine = ClinicWorkFlowProcessingStateMachine.MEDICAL_FORM_COMPLETED;
        SagaLifecycle.end();
    }

}
