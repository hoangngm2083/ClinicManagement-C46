package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemStatus;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCompletedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemCreatedEvent;
import com.clinic.c46.ExaminationFlowService.domain.event.QueueItemTakenEvent;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.QueueItemView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.QueueItemViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class QueueItemProjector {
    private final QueueItemViewRepository queryItemViewRepository;

    @EventHandler
    public void handle(QueueItemCreatedEvent event) {
        if (queryItemViewRepository.existsById(event.queueItemId()))
            return;
        QueueItemView itemView = QueueItemView.builder()
                .queueId(event.queueId())
                .id(event.queueItemId())
                .medicalFormId(event.medicalFormId())
                .serviceId(event.serviceId())
                .status(QueueItemStatus.valueOf(event.status()))
                .type(event.type())
                .build();

        itemView.markCreated();
        queryItemViewRepository.save(itemView);
    }

    @EventHandler
    public void handle(QueueItemTakenEvent event) {
        Optional<QueueItemView> itemViewOpt = queryItemViewRepository.findById(event.queueItemId());
        if (itemViewOpt.isEmpty()) {
            throw new ResourceNotFoundException("Hồ sơ không tồn tại!");
        }
        QueueItemView itemView = itemViewOpt.get();
        if (itemView.getStatus() == QueueItemStatus.IN_PROGRESS)
            return;
        itemView.setStatus(QueueItemStatus.IN_PROGRESS);
        itemView.setStaffId(event.staffId());
        itemView.markUpdated();
        queryItemViewRepository.save(itemView);
    }

    @EventHandler
    public void handle(QueueItemCompletedEvent event) {
        Optional<QueueItemView> itemViewOpt = queryItemViewRepository.findById(event.queueItemId());
        if (itemViewOpt.isEmpty()) {
            throw new ResourceNotFoundException("Hồ sơ không tồn tại!");
        }
        QueueItemView itemView = itemViewOpt.get();
        if (itemView.getStatus() == QueueItemStatus.COMPLETED)
            return;
        itemView.setStatus(QueueItemStatus.COMPLETED);
        itemView.markUpdated();
        queryItemViewRepository.save(itemView);
    }
}
