package com.clinic.c46.MedicalPackageService.domain.view;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "medical_package")
public class MedicalPackageView {
    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "package_service",
            joinColumns = @JoinColumn(name = "medical_package_id"),
            inverseJoinColumns = @JoinColumn(name = "medical_service_id")
    )
    private Set<MedicalServiceView> medicalServices = new HashSet<>();
}

