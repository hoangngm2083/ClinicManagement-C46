package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
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
    public Optional<QueueItemResponse> handle(GetQueueItemResponseByIdQuery query) {
        Optional<QueueItemDto> queueItemDtoOpt = getQueueItem(query.queueItemId());

        if (queueItemDtoOpt.isEmpty()) {
            return Optional.empty();
        }

        QueueItemDto queueItemDto = queueItemDtoOpt.get();

        // Get type-specific medical form details based on queue item type
        Optional<MedicalFormDetailsBase> medicalFormDetailsDtoOpt = getMedicalFormDetailsBase(
                queueItemDto.medicalFormId(), queueItemDto.type());

        Optional<ServiceRepDto> serviceRepDtoOpt = getService(queueItemDto.serviceId());

        QueueItemResponse queueItem = QueueItemResponse.builder()
                .queueItemId(queueItemDto.queueItemId())
                .medicalForm(medicalFormDetailsDtoOpt)
                .requestedService(serviceRepDtoOpt)
                .type(queueItemDto.type())
                .build();

        return Optional.of(queueItem);

    }

    @QueryHandler
    public boolean handle(ExistProcessingItemQuery query) {
        return queueItemViewRepository.existsByStaffIdAndStatus(query.staffId(), QueueItemStatus.IN_PROGRESS);
    }

    @QueryHandler
    public Optional<QueueItemResponse> handle(GetInProgressQueueItemByStaffIdQuery query) {
        Optional<QueueItemView> queueItemViewOpt = queueItemViewRepository.findByStaffIdAndStatus(query.staffId(),
                QueueItemStatus.IN_PROGRESS);

        if (queueItemViewOpt.isEmpty()) {
            return Optional.empty();
        }
        QueueItemView queueItemView = queueItemViewOpt.get();

        // Get type-specific medical form details based on queue item type
        Optional<MedicalFormDetailsBase> medicalFormDetailsDtoOpt = getMedicalFormDetailsBase(
                queueItemView.getMedicalFormId(), queueItemView.getType());

        Optional<ServiceRepDto> serviceRepDtoOpt = getService(queueItemView.getServiceId());

        QueueItemResponse queueItem = QueueItemResponse.builder()
                .queueItemId(queueItemView.getId())
                .medicalForm(medicalFormDetailsDtoOpt)
                .requestedService(serviceRepDtoOpt)
                .type(queueItemView.getType())
                .build();

        return Optional.of(queueItem);
    }

    /**
     * Get medical form details based on queue item type.
     * Returns MedicalFormWithExamDetailsDto for EXAM_SERVICE.
     * Returns MedicalFormWithInvoiceDetailsDto for RECEPTION_PAYMENT.
     */
    private Optional<MedicalFormDetailsBase> getMedicalFormDetailsBase(String medicalFormId,
            com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemType type) {

        switch (type) {
            case EXAM_SERVICE:
                return getMedicalFormWithExamDetails(medicalFormId).map(dto -> (MedicalFormDetailsBase) dto);

            case RECEPTION_PAYMENT:
                return getMedicalFormWithInvoiceDetails(medicalFormId).map(dto -> (MedicalFormDetailsBase) dto);

            default:
                log.warn("[QueueItemQueryHandler] Unknown queue item type: {}", type);
                return Optional.empty();
        }
    }

    private Optional<MedicalFormWithExamDetailsDto> getMedicalFormWithExamDetails(String medicalFormId) {
        GetMedicalFormDetailsByIdQuery query = new GetMedicalFormDetailsByIdQuery(medicalFormId);
        return queryGateway.query(query, ResponseTypes.optionalInstanceOf(MedicalFormWithExamDetailsDto.class))
                .join();
    }

    private Optional<MedicalFormWithInvoiceDetailsDto> getMedicalFormWithInvoiceDetails(String medicalFormId) {
        // Step 1: Get medical form to retrieve status and invoice ID
        com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery getMedicalFormQuery = new com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery(
                medicalFormId);
        Optional<com.clinic.c46.CommonService.dto.MedicalFormDto> medicalFormOpt = queryGateway.query(
                        getMedicalFormQuery,
                        ResponseTypes.optionalInstanceOf(com.clinic.c46.CommonService.dto.MedicalFormDto.class))
                .join();

        if (medicalFormOpt.isEmpty()) {
            log.warn("[QueueItemQueryHandler] Medical form not found for id: {}", medicalFormId);
            return Optional.empty();
        }

        com.clinic.c46.CommonService.dto.MedicalFormDto medicalForm = medicalFormOpt.get();

        // Step 2: Get invoice details
        // For RECEPTION_PAYMENT, the invoice is associated with the medical form
        String invoiceId = medicalForm.invoiceId();

        if (invoiceId == null || invoiceId.isEmpty()) {
            log.warn("[QueueItemQueryHandler] No invoice associated with medical form: {}", medicalFormId);
            // Return medical form without invoice details
            return Optional.of(MedicalFormWithInvoiceDetailsDto.builder()
                    .id(medicalForm.id())
                    .medicalFormStatus(medicalForm.medicalFormStatus())
                    .invoice(Optional.empty())
                    .build());
        }

        GetInvoiceDetailsByIdQuery getInvoiceQuery = new GetInvoiceDetailsByIdQuery(invoiceId);
        Optional<InvoiceDetailsDto> invoiceDetailsOpt = queryGateway.query(getInvoiceQuery,
                        ResponseTypes.optionalInstanceOf(InvoiceDetailsDto.class))
                .join();

        return Optional.of(MedicalFormWithInvoiceDetailsDto.builder()
                .id(medicalForm.id())
                .medicalFormStatus(medicalForm.medicalFormStatus())
                .invoice(invoiceDetailsOpt)
                .build());
    }

    private Optional<QueueItemDto> getQueueItem(String queueItemId) {
        GetQueueItemByIdQuery getQueueItemByIdQuery = new GetQueueItemByIdQuery(queueItemId);
        return queryGateway.query(getQueueItemByIdQuery, ResponseTypes.optionalInstanceOf(QueueItemDto.class))
                .join();
    }

    private Optional<ServiceRepDto> getService(String serviceId) {

        if (Objects.equals(serviceId, "PAYMENT_REQUEST")) {
            return Optional.of(ServiceRepDto.builder()
                    .serviceId("PAYMENT_REQUEST")
                    .name("PAYMENT_REQUEST")
                    .departmentId(QueueItemAggregate.RECEPTION_QUEUE_ID)
                    .processingPriority(9999)
                    .formTemplate("")
                    .build());
        }

        GetServiceByIdQuery getServiceByIdQuery = new GetServiceByIdQuery(serviceId);
        return queryGateway.query(getServiceByIdQuery, ResponseTypes.optionalInstanceOf(ServiceRepDto.class))
                .join();
    }

}
