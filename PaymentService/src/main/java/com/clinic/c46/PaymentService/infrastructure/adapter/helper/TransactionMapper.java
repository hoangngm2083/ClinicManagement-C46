package com.clinic.c46.PaymentService.infrastructure.adapter.helper;

import com.clinic.c46.PaymentService.application.dto.TransactionDto;
import com.clinic.c46.PaymentService.application.dto.TransactionStatusDto;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.TransactionProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")

public interface TransactionMapper {

    @Mappings({@Mapping(source = "projection.id", target = "transactionId")})
    TransactionDto toDto(TransactionProjection projection);

    @Mappings({@Mapping(source = "projection.id", target = "transactionId")})
    TransactionStatusDto toStatus(TransactionProjection projection);
}
