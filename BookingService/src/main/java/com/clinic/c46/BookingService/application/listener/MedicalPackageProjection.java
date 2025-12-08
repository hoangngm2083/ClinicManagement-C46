package com.clinic.c46.BookingService.application.listener;

import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.application.repository.ServiceRepViewRepository;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.domain.view.ServiceRepView;
import com.clinic.c46.CommonService.domain.MedicalPackagePrice;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackagePriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MedicalPackageProjection {

    private final MedicalPackageViewRepository medicalPackageRepository;
    private final ServiceRepViewRepository serviceRepViewRepository;


    @EventHandler
    public void on(MedicalPackageCreatedEvent event) {
        Set<ServiceRepView> services = new HashSet<>(serviceRepViewRepository.findAllById(event.serviceIds()));

        Set<MedicalPackagePrice> prices = new HashSet<>();
        prices.add(new MedicalPackagePrice(event.priceVersion(), event.price()));

        MedicalPackageView view = MedicalPackageView.builder()
                .medicalPackageId(event.medicalPackageId())
                .medicalPackageName(event.name())
                .prices(prices)
                .currentPriceVersion(event.priceVersion())
                .services(services)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        medicalPackageRepository.save(view);
    }
    
    @EventHandler
    public void on(MedicalPackagePriceUpdatedEvent event) {
        medicalPackageRepository.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    Set<MedicalPackagePrice> prices = view.getPrices();
                    if (prices == null) {
                        prices = new HashSet<>();
                    }
                    // Remove existing price with same version if exists
                    prices.removeIf(price -> price.getVersion() == event.newPriceVersion());
                    // Add new price
                    prices.add(new MedicalPackagePrice(event.newPriceVersion(), event.newPrice()));
                    view.setPrices(prices);
                    view.setCurrentPriceVersion(event.newPriceVersion());
                    view.setUpdatedAt(LocalDateTime.now());
                    medicalPackageRepository.save(view);
                });
    }

    @EventHandler
    public void on(MedicalPackageInfoUpdatedEvent event) {
        medicalPackageRepository.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.setMedicalPackageName(event.name());
                    view.setUpdatedAt(LocalDateTime.now());
                    medicalPackageRepository.save(view);
                });
    }

    @EventHandler
    public void on(MedicalPackageDeletedEvent event) {
        medicalPackageRepository.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.setDeletedAt(LocalDateTime.now());
                    medicalPackageRepository.save(view);
                });
    }

}
