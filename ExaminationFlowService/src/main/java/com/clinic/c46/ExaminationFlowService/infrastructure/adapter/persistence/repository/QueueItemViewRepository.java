package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemStatus;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.QueueItemView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueueItemViewRepository extends JpaRepository<QueueItemView, String> {

    boolean existsByStaffIdAndStatus(String staffId, QueueItemStatus status);

    Optional<QueueItemView> findByStaffIdAndStatus(String staffId, QueueItemStatus status);
}
