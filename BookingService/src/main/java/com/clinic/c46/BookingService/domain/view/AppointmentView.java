package com.clinic.c46.BookingService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "appointment")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AppointmentView extends BaseView {
    @Id
    private String id;
    private int shift;
    private LocalDate date;
    private String patientName;
    private String patientId;
    private String state;
    private boolean isReminded;
    
    // Snapshot price and priceVersion at booking time
    private BigDecimal snapshotPrice;
    private int snapshotPriceVersion;

    // --- Chỉ giữ relation ManyToOne ---
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medical_package_id") // tên cột foreign key trong DB
    private MedicalPackageView medicalPackage;
}

