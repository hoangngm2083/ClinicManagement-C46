package com.clinic.c46.BookingService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.CommonService.domain.MedicalPackagePrice;
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
    
    @ElementCollection
    @CollectionTable(
        name = "medical_package_rep_prices",
        joinColumns = @JoinColumn(name = "medical_package_id")
    )
    private Set<MedicalPackagePrice> prices = new HashSet<>();

    private int currentPriceVersion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "package_service_rep",
            joinColumns = @JoinColumn(name = "medical_package_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<ServiceRepView> services = new HashSet<>();

    // Helper method to get current price
    public java.math.BigDecimal getCurrentPrice() {
        if (prices == null || currentPriceVersion <= 0) {
            return null;
        }
        return prices.stream()
                .filter(price -> price.getVersion() == currentPriceVersion)
                .findFirst()
                .map(MedicalPackagePrice::getPrice)
                .orElse(null);
    }
}
