package com.clinic.c46.MedicalPackageService.domain.aggregate;


import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackageInfoCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalPackagePriceCommand;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageCreatedEvent;
import com.clinic.c46.CommonService.event.medicalPackage.MedicalPackageInfoUpdatedEvent;
import com.clinic.c46.MedicalPackageService.domain.event.MedicalPackagePriceUpdatedEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.math.BigDecimal;
import java.util.Set;

@Aggregate
@NoArgsConstructor
public class MedicalPackageAggregate {
    @AggregateIdentifier
    private String medicalPackageId;
    private String name;
    private String description;
    private Set<String> serviceIds;
    private int version;
    private BigDecimal price;


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
                .serviceIds(cmd.serviceIds())
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
        this.price = event.price();
        this.version = 1;
    }


    @CommandHandler
    public void handle(UpdateMedicalPackagePriceCommand cmd) {
        if (cmd.newPrice() == null || cmd.newPrice()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá mới không hợp lệ");
        }

        // Nếu giá không đổi -> bỏ qua (idempotent)
        if (this.price.compareTo(cmd.newPrice()) == 0) {
            return;
        }

        MedicalPackagePriceUpdatedEvent event = MedicalPackagePriceUpdatedEvent.builder()
                .medicalPackageId(cmd.medicalPackageId())
                .newPrice(cmd.newPrice())
                .newVersion(++this.version)
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalPackagePriceUpdatedEvent event) {
        this.price = event.newPrice();
        this.version = event.newVersion();
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

        if (!hasChange) return;

        MedicalPackageInfoUpdatedEvent event = MedicalPackageInfoUpdatedEvent.builder()
                .medicalPackageId(cmd.medicalPackageId())
                .name(cmd.name())
                .description(cmd.description())
                .serviceIds(cmd.serviceIds())
                .version(++this.version)
                .build();

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    public void on(MedicalPackageInfoUpdatedEvent event) {
        if (event.version() <= this.version) return;
        this.version = event.version();
        if (event.name() != null) this.name = event.name();
        if (event.description() != null) this.description = event.description();
        if (event.serviceIds() != null) this.serviceIds = event.serviceIds();
    }

}
