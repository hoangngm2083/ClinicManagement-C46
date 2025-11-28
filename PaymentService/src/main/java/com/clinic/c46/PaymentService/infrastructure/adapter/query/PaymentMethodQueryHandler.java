package com.clinic.c46.PaymentService.infrastructure.adapter.query;

import com.clinic.c46.PaymentService.application.dto.PaymentMethodDto;
import com.clinic.c46.PaymentService.application.query.GetAllPaymentMethodsQuery;
import com.clinic.c46.PaymentService.domain.aggregate.PaymentMethod;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMethodQueryHandler {


    @QueryHandler
    public List<PaymentMethodDto> handle(GetAllPaymentMethodsQuery query) {
        return Arrays.stream(PaymentMethod.values())
                .map(paymentMethod -> new PaymentMethodDto(paymentMethod.getCode(), paymentMethod.getName()))
                .collect(Collectors.toList());
    }

}
