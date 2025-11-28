package com.clinic.c46.PaymentService.infrastructure.adapter.query;

import com.clinic.c46.PaymentService.application.dto.InvoiceDto;
import com.clinic.c46.PaymentService.application.query.GetInvoiceByIdQuery;
import com.clinic.c46.PaymentService.infrastructure.adapter.helper.InvoiceMapper;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InvoiceQueryHandler {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;


    @QueryHandler
    public Optional<InvoiceDto> handle(GetInvoiceByIdQuery query) {
        return invoiceRepository.findById(query.invoiceId())
                .map(invoiceMapper::toDto);
    }


}
