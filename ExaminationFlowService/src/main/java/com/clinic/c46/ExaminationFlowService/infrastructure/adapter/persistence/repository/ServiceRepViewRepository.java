package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.ServiceRepView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepViewRepository extends JpaRepository<ServiceRepView, String> {
}
