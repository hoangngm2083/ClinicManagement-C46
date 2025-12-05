package com.clinic.c46.MedicalPackageService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "medical_package")
public class MedicalPackageView extends BaseView {
    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String image;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "package_service",
            joinColumns = @JoinColumn(name = "medical_package_id"),
            inverseJoinColumns = @JoinColumn(name = "medical_service_id")
    )
    private Set<MedicalServiceView> medicalServices = new HashSet<>();
}

