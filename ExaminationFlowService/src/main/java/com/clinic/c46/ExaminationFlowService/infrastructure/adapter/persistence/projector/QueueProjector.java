package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.CommonService.event.staff.DepartmentCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemProcessedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.QueueViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProjector {

    private final QueueViewRepository queueViewRepository;

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
    public void on(QueueItemProcessedEvent event) {
        if (queueViewRepository.isCompleted(event.queueId(), event.queueItemId())) {
            return; // idempotency
        }
        queueViewRepository.complete(event.queueId(), event.queueItemId());
    }


    @EventHandler
    public void on(DepartmentCreatedEvent event) {
        queueViewRepository.createQueue(event.departmentId());
    }

    @EventHandler
    public void on(com.clinic.c46.CommonService.event.staff.DepartmentDeletedEvent event) {
        // When department deleted, remove its queue data
        try {
            queueViewRepository.deleteQueue(event.departmentId());
            log.info("Deleted queue for department {}", event.departmentId());
        } catch (Exception e) {
            log.error("Failed to delete queue for department {}", event.departmentId(), e);
        }
    }

}
