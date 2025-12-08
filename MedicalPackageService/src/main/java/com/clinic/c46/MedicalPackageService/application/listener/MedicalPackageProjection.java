package com.clinic.c46.MedicalPackageService.application.listener;

import com.clinic.c46.CommonService.domain.MedicalPackagePrice;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import com.clinic.c46.CommonService.exception.TransientDataNotReadyException;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalServiceViewRepository;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackagePriceUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalPackageProjection {

    private final MedicalPackageViewRepository packageRepo;
    private final MedicalServiceViewRepository serviceRepo;

    @EventHandler
    @Transactional
    @Retryable(
        retryFor = {Exception.class},
        maxAttemptsExpression = "#{${retry.maxAttempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.maxDelay}/3}",
            maxDelayExpression = "#{${retry.maxDelay}}",
            multiplier = 2.0
        )
    )
    public void on(MedicalPackageCreatedEvent event) {
        log.debug("Handling MedicalPackageCreatedEvent: {}", event);

        Set<MedicalServiceView> services = new HashSet<>();

        if (event.serviceIds() != null) {
            for (String serviceId : event.serviceIds()) {
                serviceRepo.findById(serviceId)
                        .ifPresent(services::add);
            }
            // Check eventual consistency - ensure all required services exist
            if (services.size() != event.serviceIds().size()) {
                throw new TransientDataNotReadyException(
                    "Not all medical services are ready for package creation. Expected: " + event.serviceIds().size() +
                    ", Found: " + services.size()
                );
            }
        }

        Set<MedicalPackagePrice> prices = new HashSet<>();
        prices.add(new MedicalPackagePrice(event.priceVersion(), event.price()));

        MedicalPackageView view = MedicalPackageView.builder()
                .id(event.medicalPackageId())
                .name(event.name())
                .description(event.description())
                .prices(prices)
                .currentPriceVersion(event.priceVersion())
                .image(event.image())
                .medicalServices(services)
                .build();

        view.markCreated();
        packageRepo.save(view);
    }

    @EventHandler
    @Transactional
    @Retryable(
        retryFor = {Exception.class},
        maxAttemptsExpression = "#{${retry.maxAttempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.maxDelay}/3}",
            maxDelayExpression = "#{${retry.maxDelay}}",
            multiplier = 2.0
        )
    )
    public void on(MedicalPackagePriceUpdatedEvent event) {
        log.debug("Handling MedicalPackagePriceUpdatedEvent: {}", event);

        packageRepo.findById(event.medicalPackageId())
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
                    view.markUpdated();
                    packageRepo.save(view);
                });
    }

    @EventHandler
    @Transactional
    @Retryable(
        retryFor = {Exception.class},
        maxAttemptsExpression = "#{${retry.maxAttempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.maxDelay}/3}",
            maxDelayExpression = "#{${retry.maxDelay}}",
            multiplier = 2.0
        )
    )
    public void on(MedicalPackageInfoUpdatedEvent event) {
        log.debug("Handling MedicalPackageInfoUpdatedEvent: {}", event);

        packageRepo.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    if (event.name() != null) view.setName(event.name());
                    if (event.description() != null) view.setDescription(event.description());
                    if (event.image() != null) view.setImage(event.image());

                    if (event.serviceIds() != null) {
                        Set<MedicalServiceView> services = new HashSet<>();
                        for (String serviceId : event.serviceIds()) {
                            serviceRepo.findById(serviceId)
                                    .ifPresent(services::add);
                        }
                        // Check eventual consistency - ensure all required services exist
                        if (services.size() != event.serviceIds().size()) {
                            throw new TransientDataNotReadyException(
                                "Not all medical services are ready for package update. Expected: " + event.serviceIds().size() +
                                ", Found: " + services.size()
                            );
                        }
                        view.setMedicalServices(services);
                    }

                    view.markUpdated();
                    packageRepo.save(view);
                });
    }

    @EventHandler
    @Transactional
    @Retryable(
        retryFor = {Exception.class},
        maxAttemptsExpression = "#{${retry.maxAttempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.maxDelay}/3}",
            maxDelayExpression = "#{${retry.maxDelay}}",
            multiplier = 2.0
        )
    )
    public void on(MedicalPackageDeletedEvent event) {
        log.debug("Handling MedicalPackageDeletedEvent: {}", event);

        packageRepo.findById(event.medicalPackageId())
                .ifPresent(view -> {
                    view.markDeleted();
                    packageRepo.save(view);
                });
    }

    // Note: Recovery methods are not used here as Axon Framework will handle failed events
    // according to the configured error handling strategy (see axon.eventhandling.processors.*.errorHandler)
    // Failed events will be retried according to the processor configuration or moved to error handling
}
