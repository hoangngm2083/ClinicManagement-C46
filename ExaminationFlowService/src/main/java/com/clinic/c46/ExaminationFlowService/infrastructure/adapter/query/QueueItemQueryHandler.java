package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
import com.clinic.c46.CommonService.dto.MedicalFormDto;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery;
import com.clinic.c46.CommonService.query.invoice.GetInvoiceDetailsByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetServiceByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.*;
import com.clinic.c46.ExaminationFlowService.application.query.*;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemAggregate;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemStatus;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.QueueItemMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.QueueItemView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.QueueItemViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueItemQueryHandler {

    private final QueueItemViewRepository queueItemViewRepository;
    private final QueryGateway queryGateway;
    private final QueueItemMapper mapper;

    @QueryHandler
    public Optional<QueueItemDto> handle(GetQueueItemByIdQuery query) {
        return queueItemViewRepository.findById(query.queueItemId())
                .map(mapper::toDto);
    }

    @QueryHandler
    public Optional<QueueItemDetailsDto> handle(GetQueueItemDetailsByIdQuery query) {
        return queueItemViewRepository.findById(query.itemId())
                .map(mapper::toDetailsDto);

    }

    @QueryHandler
    public CompletableFuture<Optional<QueueItemResponse>> handle(GetQueueItemResponseByIdQuery query) {
        return getQueueItem(query.queueItemId()).thenCompose(queueItemDtoOpt -> {
            if (queueItemDtoOpt.isEmpty()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            QueueItemDto queueItemDto = queueItemDtoOpt.get();

            // Get type-specific medical form details based on queue item type
            CompletableFuture<Optional<MedicalFormDetailsBase>> medicalFormFuture = getMedicalFormDetailsBase(
                    queueItemDto.medicalFormId(), queueItemDto.type());

            CompletableFuture<Optional<ServiceRepDto>> serviceFuture = getService(queueItemDto.serviceId());

            // Combine both futures
            return medicalFormFuture.thenCombine(serviceFuture, (medicalFormDetailsDtoOpt, serviceRepDtoOpt) -> {
                QueueItemResponse queueItem = QueueItemResponse.builder()
                        .queueItemId(queueItemDto.queueItemId())
                        .medicalForm(medicalFormDetailsDtoOpt)
                        .requestedService(serviceRepDtoOpt)
                        .type(queueItemDto.type())
                        .build();

                return Optional.of(queueItem);
            });
        });
    }

    @QueryHandler
    public boolean handle(ExistProcessingItemQuery query) {
        return queueItemViewRepository.existsByStaffIdAndStatus(query.staffId(), QueueItemStatus.IN_PROGRESS);
    }

    @QueryHandler
    public CompletableFuture<Optional<QueueItemResponse>> handle(GetInProgressQueueItemByStaffIdQuery query) {
        Optional<QueueItemView> queueItemViewOpt = queueItemViewRepository.findByStaffIdAndStatus(query.staffId(),
                QueueItemStatus.IN_PROGRESS);

        if (queueItemViewOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        QueueItemView queueItemView = queueItemViewOpt.get();

        // Get type-specific medical form details based on queue item type
        CompletableFuture<Optional<MedicalFormDetailsBase>> medicalFormFuture = getMedicalFormDetailsBase(
                queueItemView.getMedicalFormId(), queueItemView.getType());

        CompletableFuture<Optional<ServiceRepDto>> serviceFuture = getService(queueItemView.getServiceId());

        // Combine both futures
        return medicalFormFuture.thenCombine(serviceFuture, (medicalFormDetailsDtoOpt, serviceRepDtoOpt) -> {
            QueueItemResponse queueItem = QueueItemResponse.builder()
                    .queueItemId(queueItemView.getId())
                    .medicalForm(medicalFormDetailsDtoOpt)
                    .requestedService(serviceRepDtoOpt)
                    .type(queueItemView.getType())
                    .build();

            return Optional.of(queueItem);
        });
    }

    /**
     * Get medical form details based on queue item type.
     * Returns MedicalFormWithExamDetailsDto for EXAM_SERVICE.
     * Returns MedicalFormWithInvoiceDetailsDto for RECEPTION_PAYMENT.
     */
    private CompletableFuture<Optional<MedicalFormDetailsBase>> getMedicalFormDetailsBase(String medicalFormId,
            com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType type) {

        switch (type) {
            case EXAM_SERVICE:
                return getMedicalFormWithExamDetails(medicalFormId).thenApply(
                        opt -> opt.map(dto -> (MedicalFormDetailsBase) dto));

            case RECEPTION_PAYMENT:
                return getMedicalFormWithInvoiceDetails(medicalFormId).thenApply(
                        opt -> opt.map(dto -> (MedicalFormDetailsBase) dto));

            default:
                log.warn("[QueueItemQueryHandler] Unknown queue item type: {}", type);
                return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private CompletableFuture<Optional<MedicalFormWithExamDetailsDto>> getMedicalFormWithExamDetails(
            String medicalFormId) {
        // 1. Find MedicalFormDTO

        CompletableFuture<Optional<MedicalFormDto>> medicalFormOptFeature = queryGateway.query(
                new GetMedicalFormByIdQuery(medicalFormId), ResponseTypes.optionalInstanceOf(MedicalFormDto.class));

        return medicalFormOptFeature.thenCompose((medicalFormDtoOpt) -> {
            if (medicalFormDtoOpt.isEmpty()) {
                // Need to return CompletableFuture<Optional<MedicalFormWithExamDetailsDto>> if
                // view is not found
                return CompletableFuture.completedFuture(Optional.empty());
            }

            MedicalFormDto medicalFormDto = medicalFormDtoOpt.get();

            GetExaminationByIdQuery getExaminationByIdQuery = GetExaminationByIdQuery.builder()
                    .examinationId(medicalFormDto.examinationId())
                    .build();

            // 3. Make an Examination call via the Query Gateway
            CompletableFuture<Optional<ExamDetailsDto>> examinationFuture = queryGateway.query(getExaminationByIdQuery,
                    ResponseTypes.optionalInstanceOf(ExamDetailsDto.class));

            // 4. When examinationFuture completes, use the result to combine with 'view'
            // and create the final DTO (use the view in this valid range)
            return examinationFuture.thenApply(examinationOpt -> Optional.of(MedicalFormWithExamDetailsDto.builder()
                    .id(medicalFormDto.id())
                    .medicalFormStatus(medicalFormDto.medicalFormStatus())
                    .examination(examinationOpt)
                    .build()));
        });

    }

    private CompletableFuture<Optional<MedicalFormWithInvoiceDetailsDto>> getMedicalFormWithInvoiceDetails(
            String medicalFormId) {
        // Step 1: Get medical form to retrieve status and invoice ID
        GetMedicalFormByIdQuery getMedicalFormQuery = new GetMedicalFormByIdQuery(medicalFormId);
        CompletableFuture<Optional<MedicalFormDto>> medicalFormFuture = queryGateway.query(getMedicalFormQuery,
                ResponseTypes.optionalInstanceOf(MedicalFormDto.class));

        return medicalFormFuture.thenCompose(medicalFormOpt -> {
            if (medicalFormOpt.isEmpty()) {
                log.warn("[QueueItemQueryHandler] Medical form not found for id: {}", medicalFormId);
                return CompletableFuture.completedFuture(Optional.empty());
            }

            MedicalFormDto medicalForm = medicalFormOpt.get();

            // Step 2: Get invoice details
            // For RECEPTION_PAYMENT, the invoice is associated with the medical form
            String invoiceId = medicalForm.invoiceId();

            if (invoiceId == null || invoiceId.isEmpty()) {
                log.warn("[QueueItemQueryHandler] No invoice associated with medical form: {}", medicalFormId);
                // Return medical form without invoice details
                return CompletableFuture.completedFuture(Optional.of(MedicalFormWithInvoiceDetailsDto.builder()
                        .id(medicalForm.id())
                        .medicalFormStatus(medicalForm.medicalFormStatus())
                        .invoice(Optional.empty())
                        .build()));
            }

            GetInvoiceDetailsByIdQuery getInvoiceQuery = new GetInvoiceDetailsByIdQuery(invoiceId);
            CompletableFuture<Optional<InvoiceDetailsDto>> invoiceFuture = queryGateway.query(getInvoiceQuery,
                    ResponseTypes.optionalInstanceOf(InvoiceDetailsDto.class));

            return invoiceFuture.thenApply(invoiceDetailsOpt -> Optional.of(MedicalFormWithInvoiceDetailsDto.builder()
                    .id(medicalForm.id())
                    .medicalFormStatus(medicalForm.medicalFormStatus())
                    .invoice(invoiceDetailsOpt)
                    .build()));
        });
    }

    private CompletableFuture<Optional<QueueItemDto>> getQueueItem(String queueItemId) {
        GetQueueItemByIdQuery getQueueItemByIdQuery = new GetQueueItemByIdQuery(queueItemId);
        return queryGateway.query(getQueueItemByIdQuery, ResponseTypes.optionalInstanceOf(QueueItemDto.class));
    }

    private CompletableFuture<Optional<ServiceRepDto>> getService(String serviceId) {

        if (Objects.equals(serviceId, "PAYMENT_REQUEST")) {
            return CompletableFuture.completedFuture(Optional.of(ServiceRepDto.builder()
                    .serviceId("PAYMENT_REQUEST")
                    .name("PAYMENT_REQUEST")
                    .departmentId(QueueItemAggregate.RECEPTION_QUEUE_ID)
                    .processingPriority(9999)
                    .formTemplate(null)
                    .build()));
        }

        GetServiceByIdQuery getServiceByIdQuery = new GetServiceByIdQuery(serviceId);
        return queryGateway.query(getServiceByIdQuery, ResponseTypes.optionalInstanceOf(ServiceRepDto.class));
    }

}
