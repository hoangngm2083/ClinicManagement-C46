package com.clinic.c46.BookingService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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

}
