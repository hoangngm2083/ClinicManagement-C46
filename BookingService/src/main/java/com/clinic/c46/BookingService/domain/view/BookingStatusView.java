package com.clinic.c46.BookingService.domain.view;

import com.clinic.c46.BookingService.domain.enums.BookingStatus;
import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_status")
@Getter
@NoArgsConstructor
public class BookingStatusView extends BaseView {
    @Id
    private String bookingId;
    private String appointmentId;
    private String status;
    private String message;


    public BookingStatusView(String bookingId) {
        this.bookingId = bookingId;
        this.status = BookingStatus.PENDING.name();
        super.setCreatedAt(LocalDateTime.now());
        super.setUpdatedAt(LocalDateTime.now());

    }

    public void approve(String appointmentId) {
        this.status = BookingStatus.APPROVED.name();
        this.message = "Đặt lịch thành công!";
        this.appointmentId = appointmentId;
        super.setUpdatedAt(LocalDateTime.now());
    }

    public void reject(String message) {
        this.status = BookingStatus.REJECTED.name();
        this.message = message;
        super.setUpdatedAt(LocalDateTime.now());
    }
}
