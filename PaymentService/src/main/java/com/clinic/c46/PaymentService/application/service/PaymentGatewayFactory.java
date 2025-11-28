package com.clinic.c46.PaymentService.application.service;

import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;

public interface PaymentGatewayFactory {

    PaymentGateway get(PaymentMethod paymentMethod);


}
