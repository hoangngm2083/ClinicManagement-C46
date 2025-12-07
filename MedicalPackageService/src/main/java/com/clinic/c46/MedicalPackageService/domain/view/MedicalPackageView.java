package com.clinic.c46.MedicalPackageService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.CommonService.domain.MedicalPackagePrice;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@SuperBuilder
@Table(name = "medical_package")
public class MedicalPackageView extends BaseView {
    @Id
    private String id;
    private String name;
    private String description;

    @ElementCollection
    @CollectionTable(
        name = "medical_package_prices",
        joinColumns = @JoinColumn(name = "medical_package_id")
    )
    private Set<MedicalPackagePrice> prices = new HashSet<>();

    private int currentPriceVersion;

    @Column(columnDefinition = "TEXT")
    private String image;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "package_service", joinColumns = @JoinColumn(name = "medical_package_id"), inverseJoinColumns = @JoinColumn(name = "medical_service_id"))
    private Set<MedicalServiceView> medicalServices = new HashSet<>();

    // Manual getters for Lombok compatibility
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImage() { return image; }
    public int getCurrentPriceVersion() { return currentPriceVersion; }
    public Set<MedicalPackagePrice> getPrices() { return prices; }
    public Set<MedicalServiceView> getMedicalServices() { return medicalServices; }

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

