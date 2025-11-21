package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.QueueItemMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.QueueItemViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueItemQueryHandler {

    private final QueueItemViewRepository queueItemViewRepository;
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
}
