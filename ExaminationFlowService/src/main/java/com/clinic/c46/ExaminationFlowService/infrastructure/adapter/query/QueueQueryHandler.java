package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;


import com.clinic.c46.ExaminationFlowService.application.query.GetItemIdOfTopQueueQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueSizeQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.QueueViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QueueQueryHandler {

    private final QueueViewRepository queueViewRepository;

    @QueryHandler
    public Optional<String> handle(GetItemIdOfTopQueueQuery query) {
        return queueViewRepository.peekHead(query.queueId());
    }

    @QueryHandler
    public Long handle(GetQueueSizeQuery query) {
        return queueViewRepository.getQueueSize(query.queueId());
    }

}
