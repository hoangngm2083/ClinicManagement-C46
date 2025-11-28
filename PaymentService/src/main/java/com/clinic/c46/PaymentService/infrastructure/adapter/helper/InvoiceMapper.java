package com.clinic.c46.PaymentService.infrastructure.adapter.helper;

import com.clinic.c46.PaymentService.application.dto.InvoiceDto;
import com.clinic.c46.PaymentService.application.dto.MedicalPackageRepDto;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.InvoiceProjection;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.MedicalPackageRep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")

public interface InvoiceMapper {

    MedicalPackageRepDto toDto(MedicalPackageRep projection);

    @Mappings({@Mapping(source = "projection.id", target = "invoiceId")})
    InvoiceDto toDto(InvoiceProjection projection);
}
