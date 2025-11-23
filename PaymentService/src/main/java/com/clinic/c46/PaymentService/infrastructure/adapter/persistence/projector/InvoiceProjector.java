package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projector;


import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceProjector {

    private final InvoiceRepository invoiceRepository;

    @EventHandler
    public void on(InvoiceCreatedEvent event)
    {

    }


}
