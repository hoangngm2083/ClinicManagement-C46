package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.query.medicalPackage.GetServiceByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.*;
import com.clinic.c46.ExaminationFlowService.application.query.*;
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

        Optional<MedicalFormDetailsDto> medicalFormDetailsDtoOpt = getMedicalFormDetails(queueItemDto.medicalFormId());

        Optional<ServiceRepDto> serviceRepDtoOpt = getService(queueItemDto.serviceId());

        QueueItemResponse queueItem = QueueItemResponse.builder()
                .queueItemId(queueItemDto.queueItemId())
                .medicalForm(medicalFormDetailsDtoOpt)
                .requestedService(serviceRepDtoOpt)
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
        Optional<MedicalFormDetailsDto> medicalFormDetailsDtoOpt = getMedicalFormDetails(
                queueItemView.getMedicalFormId());
        Optional<ServiceRepDto> serviceRepDtoOpt = getService(queueItemView.getServiceId());

        QueueItemResponse queueItem = QueueItemResponse.builder()
                .queueItemId(queueItemView.getId())
                .medicalForm(medicalFormDetailsDtoOpt)
                .requestedService(serviceRepDtoOpt)
                .build();

        return Optional.of(queueItem);
    }

    private Optional<MedicalFormDetailsDto> getMedicalFormDetails(String medicalFormId) {
        GetMedicalFormDetailsByIdQuery getMedicalFormDetailsByIdQuery = new GetMedicalFormDetailsByIdQuery(
                medicalFormId);
        return queryGateway.query(getMedicalFormDetailsByIdQuery,
                        ResponseTypes.optionalInstanceOf(MedicalFormDetailsDto.class))
                .join();
    }

    private Optional<QueueItemDto> getQueueItem(String queueItemId) {
        GetQueueItemByIdQuery getQueueItemByIdQuery = new GetQueueItemByIdQuery(queueItemId);
        return queryGateway.query(getQueueItemByIdQuery, ResponseTypes.optionalInstanceOf(QueueItemDto.class))
                .join();
    }

    private Optional<ServiceRepDto> getService(String serviceId) {
        GetServiceByIdQuery getServiceByIdQuery = new GetServiceByIdQuery(serviceId);
        return queryGateway.query(getServiceByIdQuery, ResponseTypes.optionalInstanceOf(ServiceRepDto.class))
                .join();
    }
}
