package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;
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
    private BigDecimal price;
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "package_service_mapping", joinColumns = @JoinColumn(name = "package_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<ServiceRepView> services;
}
