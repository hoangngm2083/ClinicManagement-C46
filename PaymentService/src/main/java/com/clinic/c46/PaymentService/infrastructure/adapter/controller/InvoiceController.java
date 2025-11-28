package com.clinic.c46.PaymentService.infrastructure.adapter.controller;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.PaymentService.application.dto.InvoiceDto;
import com.clinic.c46.PaymentService.application.query.GetInvoiceByIdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payment/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final QueryGateway queryGateway;

    @GetMapping("/{invoiceId}")
    public CompletableFuture<ResponseEntity<InvoiceDto>> getInvoice(@PathVariable String invoiceId) {
        log.info("Getting invoice: {}", invoiceId);

        return queryGateway.query(new GetInvoiceByIdQuery(invoiceId),
                        ResponseTypes.optionalInstanceOf(InvoiceDto.class))
                .thenApply(result -> {
                    InvoiceDto invoiceDto = result.orElseThrow(() -> new ResourceNotFoundException("Hóa đơn"));
                    return ResponseEntity.ok(invoiceDto);
                });
    }
}
