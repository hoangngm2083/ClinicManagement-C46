package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository;

import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.MedicalFormView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalFormViewRepository extends JpaRepository<MedicalFormView, String>, JpaSpecificationExecutor<MedicalFormView> {

}
