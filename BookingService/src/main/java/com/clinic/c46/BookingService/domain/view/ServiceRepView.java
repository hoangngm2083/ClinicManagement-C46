package com.clinic.c46.BookingService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "service_rep")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ServiceRepView extends BaseView {
    @Id
    private String id;
    private String name;
}
