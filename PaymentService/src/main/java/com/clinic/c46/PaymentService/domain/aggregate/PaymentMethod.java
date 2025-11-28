package com.clinic.c46.PaymentService.domain.aggregate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PaymentMethod {
    CASH(3, "Tiền mặt"),
//    CARD(0, "Thẻ"), MOMO(1, "Ví điện tử Momo"), BANK(2, "Ngân hàng"),
    VNPAY(4, "Ví điện tử VNPay");
    private final int code;
    private final String name;
}