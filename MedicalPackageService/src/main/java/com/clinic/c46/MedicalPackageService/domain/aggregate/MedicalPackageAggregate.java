package com.clinic.c46.MedicalPackageService.domain.aggregate;


import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageDeletedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackagePriceUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Aggregate
@NoArgsConstructor
public class MedicalPackageAggregate {
    @AggregateIdentifier
    private String medicalPackageId;
    private String name;
    private String description;
    private Set<String> serviceIds;
    private Map<Integer, BigDecimal> prices = new HashMap<>();
    private int currentPriceVersion;
    private String image;
    private boolean isDeleted;


    @CommandHandler
    public MedicalPackageAggregate(CreateMedicalPackageCommand cmd) {
        // validate
        if (cmd.price() == null || cmd.price()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá gói khám phải lớn hơn hoặc bằng 0");
        }
        // mapping to event
        MedicalPackageCreatedEvent event = MedicalPackageCreatedEvent.builder()
                .medicalPackageId(cmd.medicalPackageId())
                .name(cmd.name())
                .description(cmd.description())
                .price(cmd.price())
                .image(cmd.image())
                .serviceIds(cmd.serviceIds())
                .priceVersion(1)
                .build();

        // apply
        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalPackageCreatedEvent event) {
        this.medicalPackageId = event.medicalPackageId();
        this.name = event.name();
        this.description = event.description();
        this.serviceIds = event.serviceIds();
        this.prices = new HashMap<>();
        this.prices.put(event.priceVersion(), event.price());
        this.currentPriceVersion = event.priceVersion();
        this.image = event.image();
    }


    @CommandHandler
    public void handle(UpdateMedicalPackagePriceCommand cmd) {
        if (cmd.newPrice() == null || cmd.newPrice()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá mới không hợp lệ");
        }

        BigDecimal currentPrice = this.prices.get(this.currentPriceVersion);
        if (currentPrice != null && currentPrice.compareTo(cmd.newPrice()) == 0) {
            return;
        }

        int newPriceVersion = this.currentPriceVersion + 1;
        MedicalPackagePriceUpdatedEvent event = MedicalPackagePriceUpdatedEvent.builder()
                .medicalPackageId(cmd.medicalPackageId())
                .newPrice(cmd.newPrice())
                .newPriceVersion(newPriceVersion)
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalPackagePriceUpdatedEvent event) {
        this.prices.put(event.newPriceVersion(), event.newPrice());
        this.currentPriceVersion = event.newPriceVersion();
    }

    @CommandHandler
    public void handle(UpdateMedicalPackageInfoCommand cmd) {
        boolean hasChange = cmd.name() != null && !cmd.name()
                .equals(this.name);

        if (cmd.description() != null && !cmd.description()
                .equals(this.description)) {
            hasChange = true;
        }
        if (cmd.serviceIds() != null && !cmd.serviceIds()
                .equals(this.serviceIds)) {
            hasChange = true;
        }

        if (cmd.image() != null && !cmd.image()
                .equals(this.image)) {
            hasChange = true;
        }

        if (!hasChange) return;

        MedicalPackageInfoUpdatedEvent event = MedicalPackageInfoUpdatedEvent.builder()
                .medicalPackageId(cmd.medicalPackageId())
                .name(cmd.name())
                .description(cmd.description())
                .serviceIds(cmd.serviceIds())
                .image(cmd.image())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalPackageInfoUpdatedEvent event) {
        if (event.name() != null) this.name = event.name();
        if (event.description() != null) this.description = event.description();
        if (event.serviceIds() != null) this.serviceIds = event.serviceIds();
        if (event.image() != null) this.image = event.image();
    }

    @CommandHandler
    public void handle(DeleteMedicalPackageCommand cmd) {
        if (this.isDeleted) {
            throw new IllegalStateException("Gói khám đã bị xóa");
        }

        MedicalPackageDeletedEvent event = MedicalPackageDeletedEvent.builder()
                .medicalPackageId(cmd.medicalPackageId())
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalPackageDeletedEvent event) {
        this.isDeleted = true;
    }

}


