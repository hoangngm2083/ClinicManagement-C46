package com.clinic.c46.PaymentService.infrastructure.adapter.gateway;

import com.clinic.c46.PaymentService.application.service.PaymentGateway;
import com.clinic.c46.PaymentService.application.service.PaymentGatewayFactory;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayFactoryImpl implements PaymentGatewayFactory {

    @Override
    public PaymentGateway get(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case VNPAY -> new VNPayPaymentGateway();
            case MOMO -> new MomoPaymentGateway();
            default -> new CashPaymentGateway();
        };
    }
}
