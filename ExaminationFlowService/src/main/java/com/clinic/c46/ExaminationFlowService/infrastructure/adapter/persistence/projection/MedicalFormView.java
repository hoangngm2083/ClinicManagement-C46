package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "medical_form")
@Cacheable
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class MedicalFormView extends BaseView {
    @Id
    private String id;
    private String patientId;
    private String medicalFormStatus;
    private String examinationId;
    private String invoiceId;
    private Set<String> packageIds;
}
