package com.clinic.c46.BookingService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SlotLockedView extends BaseView {
    @Id
    private String fingerprint;
    private String slotId;
    private String appointmentId;
}

