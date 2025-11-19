package com.clinic.c46.ExaminationService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "doctor_rep_view")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRepView extends BaseView {
    @Id
    private String id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String eSignature;
}
