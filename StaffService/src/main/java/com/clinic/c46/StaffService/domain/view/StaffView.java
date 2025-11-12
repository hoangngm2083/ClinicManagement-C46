package com.clinic.c46.StaffService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.StaffService.domain.enums.Role;
import com.clinic.c46.StaffService.domain.event.StaffCreatedEvent;
import com.clinic.c46.StaffService.domain.event.StaffInfoUpdatedEvent;
import com.clinic.c46.StaffService.domain.valueObject.DayOff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "staff_view")
@NoArgsConstructor
@Getter
@Slf4j
public class StaffView extends BaseView {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String image;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(columnDefinition = "TEXT")
    private String eSignature;

    private String departmentId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "staff_day_offs", joinColumns = @JoinColumn(name = "staff_id"))
    private List<DayOff> dayOffs = new ArrayList<>();


    public StaffView(StaffCreatedEvent event) {
        this.id = event.staffId();
        this.name = event.name();
        this.email = event.email();
        this.phone = event.phone();
        this.description = event.description();
        this.image = event.image();
        this.role = event.role();
        this.eSignature = event.eSignature();
        this.departmentId = event.departmentId();
        this.dayOffs = new ArrayList<>();
        markCreated();
    }

    public void handleUpdate(StaffInfoUpdatedEvent event) {
        this.name = event.name();
        this.phone = event.phone();
        this.description = event.description();
        this.image = event.image();
        this.role = event.role();
        this.eSignature = event.eSignature();
        this.departmentId = event.departmentId();
        markUpdated();
    }

    public void handleDayOffsRequest(Set<DayOff> dayOffs) {
        log.warn("=== INCOMING DAYOFFS: {}", dayOffs);
        log.warn("=== BEFORE {}", this.dayOffs);
        log.debug("=== BEFORE {}", this.dayOffs);
        this.dayOffs.addAll(dayOffs);
        log.warn("=== AFTER {}", this.dayOffs);
        log.debug("=== AFTER {}", this.dayOffs);
        markUpdated();
    }

    public void handleDelete() {
        markDeleted();
    }

    public boolean isDeleted() {
        return this.getDeletedAt() != null;
    }
}
