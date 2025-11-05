package com.clinic.c46.BookingService.domain.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class SlotView {
    @Id
    private String slotId;
    private String medicalPackageId;
    private String bookingId;
    private int shift;
    private int maxQuantity;
    private int remainingQuantity;


    public void lock(){
        if (this.remainingQuantity <= 0){
            throw new IllegalStateException("Remaining quantity less than 0");
        }
        this.remainingQuantity--;
    }

    public void release(){
        if (this.remainingQuantity >= maxQuantity){
            throw new IllegalStateException("Remaining quantity greater than maximum quantity");
        }
        this.remainingQuantity++;
    }
}
