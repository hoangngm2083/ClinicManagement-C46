package com.clinic.c46.MedicalPackageService.domain.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "medical_service")
public class MedicalServiceView {

    @Id
    private String id;
    private String name;
    private String description;
    private String departmentName;
    private String departmentId;
    private int processingPriority;
}


