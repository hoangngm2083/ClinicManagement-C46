package com.clinic.c46.BookingService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Table(name = "medical_package_rep")
public class MedicalPackageView extends BaseView {

    @Id
    private String medicalPackageId; // Changed to String for consistency
    private String medicalPackageName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "package_service_rep",
            joinColumns = @JoinColumn(name = "medical_package_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<ServiceRepView> services = new HashSet<>();
}
