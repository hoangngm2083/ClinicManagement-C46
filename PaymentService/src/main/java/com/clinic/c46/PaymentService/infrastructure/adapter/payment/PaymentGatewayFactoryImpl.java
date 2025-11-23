package com.clinic.c46.PaymentService.infrastructure.adapter.payment;

import com.clinic.c46.PaymentService.application.service.PaymentGateway;
import com.clinic.c46.PaymentService.application.service.PaymentGatewayFactory;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentGatewayFactoryImpl implements PaymentGatewayFactory {
    private final VNPayPaymentGateway vNPayPaymentGateway;

    @Override
    public PaymentGateway get(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case VNPAY -> vNPayPaymentGateway;
            default -> vNPayPaymentGateway;
        };
    }
}
