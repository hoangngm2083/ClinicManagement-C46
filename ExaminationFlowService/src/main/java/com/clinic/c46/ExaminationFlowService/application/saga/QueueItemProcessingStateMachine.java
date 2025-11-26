package com.clinic.c46.ExaminationFlowService.application.saga;

/**
 * Unified state machine cho xử lý queue item (cả EXAM_SERVICE và
 * RECEPTION_PAYMENT)
 * 
 * Cho EXAM_SERVICE flow:
 * PENDING_SEND_ITEM -> ITEM_SENT -> PENDING_CREATE_RESULT -> RESULT_CREATED ->
 * COMPLETED
 * 
 * Cho RECEPTION_PAYMENT flow:
 * PAYMENT_PENDING -> WAITING_FOR_PAYMENT -> PAYMENT_COMPLETED -> COMPLETED
 */
public enum QueueItemProcessingStateMachine {
    // Common states
    PENDING_SEND_ITEM, // Chờ gửi item cho nhân viên

    // EXAM_SERVICE specific states
    ITEM_SENT, // Item đã gửi cho nhân viên khám
    PENDING_CREATE_RESULT, // Chờ tạo kết quả khám
    RESULT_CREATED, // Kết quả khám đã tạo

    // RECEPTION_PAYMENT specific states
    PAYMENT_PENDING, // Chờ lấy thông tin thanh toán
    WAITING_FOR_PAYMENT, // Chờ thanh toán từ PaymentService
    PAYMENT_COMPLETED, // Thanh toán đã hoàn thành

    // Common final state
    COMPLETED // Saga kết thúc
}
