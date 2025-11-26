package com.clinic.c46.ExaminationFlowService.domain.aggregate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum QueueItemType {
    EXAM_SERVICE("exam_service"), // Dành cho các dịch vụ khám/cận lâm sàng (Siêu âm, Xét nghiệm...)
    RECEPTION_PAYMENT("reception_payment"); // Dành cho bước thanh toán cuối cùng tại Lễ tân

    private final String value;

    public String getValue() {
        return value;
    }

    public static QueueItemType fromValue(String value) {
        for (QueueItemType type : QueueItemType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown QueueItemType: " + value);
    }
}
