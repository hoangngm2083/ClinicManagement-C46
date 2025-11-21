package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.QueueItemView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueItemViewRepository extends JpaRepository<QueueItemView, String> {
}
