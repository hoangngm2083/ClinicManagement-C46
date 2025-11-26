package com.clinic.c46.ExaminationFlowService.application.saga;

/**
 * Trạng thái xử lý thanh toán tại bàn lễ tân
 */
public enum ReceptionPaymentProcessingSagaStateMachine {
    PAYMENT_PENDING, // Đang chờ lấy thông tin thanh toán
    WAITING_FOR_PAYMENT, // Chờ hoàn thành thanh toán từ PaymentService
    PAYMENT_COMPLETED, // Thanh toán đã hoàn thành
    COMPLETED // Saga kết thúc
}
