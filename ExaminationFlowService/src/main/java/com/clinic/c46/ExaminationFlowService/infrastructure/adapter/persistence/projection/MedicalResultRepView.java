package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.*;

@Entity
@Table(name = "medical_result_rep")
public class MedicalResultRepView extends BaseView {
    @Id
    private String id;
    private String serviceId;
    @Column(columnDefinition = "TEXT")
    private String data;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicalFormId", insertable = false, updatable = false)
    private MedicalFormView medicalForm;
}
