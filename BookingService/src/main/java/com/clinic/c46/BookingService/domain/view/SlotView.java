package com.clinic.c46.BookingService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "slot")
public class SlotView extends BaseView {
    @Id
    private String slotId;
    private String medicalPackageId;
    private LocalDate date;
    private int shift;
    private int maxQuantity;
    private int remainingQuantity;

    public SlotView(String slotId, String medicalPackageId, int shift, int maxQuantity,LocalDate date) {
        this.slotId = slotId;
        this.medicalPackageId = medicalPackageId;
        this.shift = shift;
        this.maxQuantity = maxQuantity;
        this.remainingQuantity = this.maxQuantity;
        this.date = date;
        this.setCreatedAt(LocalDateTime.now());
        this.setUpdatedAt(LocalDateTime.now());
    }


    public void lock() {
        if (this.remainingQuantity <= 0) {
            throw new IllegalStateException("Remaining quantity less than 0");
        }
        this.remainingQuantity--;
    }

    public void release() {
        if (this.remainingQuantity >= maxQuantity) {
            throw new IllegalStateException("Remaining quantity greater than maximum quantity");
        }
        this.remainingQuantity++;
    }

    public void updateMaxQuantity(int newMaxQuantity, int difference) {
        this.maxQuantity = newMaxQuantity;
        this.remainingQuantity += difference;
    }
}
