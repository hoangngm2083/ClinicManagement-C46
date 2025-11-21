package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.CommonService.event.staff.DepartmentCreatedEvent;
import com.clinic.c46.CommonService.event.staff.DepartmentDeletedEvent;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDetailsDto;
import com.clinic.c46.ExaminationFlowService.application.query.GetQueueItemDetailsByIdQuery;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCompletedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.QueueViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProjector {

    private final QueueViewRepository queueViewRepository;
    private final QueryGateway queryGateway;

    @EventHandler
    public void on(QueueItemCreatedEvent event) {
        if (queueViewRepository.isInQueue(event.queueId(), event.queueItemId())) {
            return;
        }
        queueViewRepository.enqueueToTail(event.queueId(), event.queueItemId());
    }

    @EventHandler
    public void on(QueueItemTakenEvent event) {
        if (queueViewRepository.isInProgress(event.queueItemId())) {
            return; // idempotency
        }
        queueViewRepository.handleTakeNext(event.queueId());
    }

    @EventHandler
    public void on(QueueItemCompletedEvent event) {

        GetQueueItemDetailsByIdQuery getQueueItemByIdQuery = new GetQueueItemDetailsByIdQuery(event.queueItemId());
        Optional<QueueItemDetailsDto> queueItemDetailsDtoOptional = queryGateway.query(getQueueItemByIdQuery,
                        ResponseTypes.optionalInstanceOf(QueueItemDetailsDto.class))
                .join();
        if (queueItemDetailsDtoOptional.isEmpty()) {
            throw new ResourceNotFoundException("Hồ sơ trong hàng đợi");
        }
        QueueItemDetailsDto queueItemDetailsDto = queueItemDetailsDtoOptional.get();

        if (queueViewRepository.isCompleted(queueItemDetailsDto.queueId(), event.queueItemId())) {
            return; // idempotency
        }

        queueViewRepository.complete(queueItemDetailsDto.queueId(), event.queueItemId());
    }


    @EventHandler
    public void on(DepartmentCreatedEvent event) {
        queueViewRepository.createQueue(event.departmentId());
    }

    @EventHandler
    public void on(DepartmentDeletedEvent event) {
        // When department deleted, remove its queue data
        try {
            queueViewRepository.deleteQueue(event.departmentId());
            log.info("Deleted queue for department {}", event.departmentId());
        } catch (Exception e) {
            log.error("Failed to delete queue for department {}", event.departmentId(), e);
        }
    }

}
