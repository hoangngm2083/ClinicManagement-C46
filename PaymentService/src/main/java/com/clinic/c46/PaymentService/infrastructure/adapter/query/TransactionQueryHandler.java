package com.clinic.c46.PaymentService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.PaymentService.application.dto.TransactionDto;
import com.clinic.c46.PaymentService.application.dto.TransactionStatusDto;
import com.clinic.c46.PaymentService.application.query.ExistsTransactionOfInvoiceQuery;
import com.clinic.c46.PaymentService.application.query.GetAllTransactionsQuery;
import com.clinic.c46.PaymentService.application.query.GetTransactionByIdQuery;
import com.clinic.c46.PaymentService.application.query.GetTransactionStatusQuery;
import com.clinic.c46.PaymentService.infrastructure.adapter.helper.TransactionMapper;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.TransactionProjection;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransactionQueryHandler {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final SpecificationBuilder specificationBuilder;


    @QueryHandler
    public Optional<TransactionDto> handle(GetTransactionByIdQuery query) {
        return transactionRepository.findById(query.transactionId())
                .map(transactionMapper::toDto);
    }

    @QueryHandler
    public Optional<TransactionStatusDto> handle(GetTransactionStatusQuery query) {
        return transactionRepository.findById(query.transactionId())
                .map(transactionMapper::toStatus);
    }

    @QueryHandler
    public Boolean handle(ExistsTransactionOfInvoiceQuery query) {
        return transactionRepository.existsByInvoiceId(query.invoiceId());
    }

    @QueryHandler
    public List<TransactionDto> handle(GetAllTransactionsQuery query) {
        Specification<TransactionProjection> equalsSpec = specificationBuilder.fieldEquals("invoiceId",
                query.invoiceId());
        Specification<TransactionProjection> inSpec = specificationBuilder.in("status",
                new ArrayList<>(query.statuses()));

        Specification<TransactionProjection> finalSpec = Specification.allOf(equalsSpec)
                .and(inSpec);
        List<TransactionProjection> transactionProjections = transactionRepository.findAll(finalSpec);

        return transactionProjections.stream()
                .map(transactionMapper::toDto)
                .toList();
    }


}
