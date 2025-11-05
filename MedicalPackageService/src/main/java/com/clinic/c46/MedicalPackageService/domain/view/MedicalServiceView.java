package com.clinic.c46.MedicalPackageService.domain.view;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "medical_service")
public class MedicalServiceView {

    @Id
    private String id;
    private String name;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentView department;

    @ManyToMany(mappedBy = "medicalServices", fetch = FetchType.LAZY)
    private Set<MedicalPackageView> medicalPackages = new HashSet<>();
}


