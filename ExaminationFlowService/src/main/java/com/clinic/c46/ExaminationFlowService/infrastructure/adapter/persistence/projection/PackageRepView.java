package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.CommonService.domain.MedicalPackagePrice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "medical_package_rep")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PackageRepView extends BaseView {
    @Id
    private String id;
    
    @ElementCollection
    @CollectionTable(
        name = "package_rep_prices",
        joinColumns = @JoinColumn(name = "package_id")
    )
    private Set<MedicalPackagePrice> prices = new HashSet<>();

    private int currentPriceVersion;
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "package_service_mapping", joinColumns = @JoinColumn(name = "package_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceRepView> services;

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
