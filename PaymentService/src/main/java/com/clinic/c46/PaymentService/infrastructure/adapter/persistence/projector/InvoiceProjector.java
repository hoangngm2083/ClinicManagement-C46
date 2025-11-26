package com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projector;


import com.clinic.c46.CommonService.dto.MedicalFormDto;
import com.clinic.c46.CommonService.dto.MedicalPackageDTO;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.examinationFlow.GetMedicalFormByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesInIdsQuery;
import com.clinic.c46.PaymentService.domain.aggregate.InvoiceStatus;
import com.clinic.c46.CommonService.event.payment.InvoiceCreatedEvent;
import com.clinic.c46.PaymentService.domain.event.InvoicePaidEvent;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.InvoiceProjection;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.MedicalPackageRep;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvoiceProjector {

    private final InvoiceRepository invoiceRepository;
    private final QueryGateway queryGateway;

    @EventHandler
    public void on(InvoiceCreatedEvent event) {
        CompletableFuture<InvoiceProjection> projectionFuture = fetchMedicalForm(event.medicalFormId()).thenCompose(
                medicalFormDto -> fetchAndProcessPackages(medicalFormDto, event.invoiceId()));
        projectionFuture.thenAccept(this::saveProjection);
    }

    @EventHandler
    public void on(InvoicePaidEvent event) {
        invoiceRepository.findById(event.getInvoiceId())
                .ifPresent(invoice -> {
                    invoice.setStatus(InvoiceStatus.PAYED);
                    invoice.markUpdated();
                    invoiceRepository.save(invoice);
                });
    }


    private CompletableFuture<MedicalFormDto> fetchMedicalForm(String medicalFormId) {
        return queryGateway.query(new GetMedicalFormByIdQuery(medicalFormId),
                        ResponseTypes.optionalInstanceOf(MedicalFormDto.class))
                .thenApply(medicalFormOpt -> medicalFormOpt.orElseThrow(
                        () -> new ResourceNotFoundException("Phiếu khám bệnh không tìm thấy")));
    }


    private CompletableFuture<InvoiceProjection> fetchAndProcessPackages(MedicalFormDto medicalFormDto,
            String invoiceId) {

        return queryGateway.query(new GetAllPackagesInIdsQuery(medicalFormDto.packageIds()),
                        ResponseTypes.multipleInstancesOf(MedicalPackageDTO.class))
                .thenApply(
                        medicalPackageDTOs -> createProjectionFromForm(medicalFormDto, invoiceId, medicalPackageDTOs));
    }


    private InvoiceProjection createProjectionFromForm(MedicalFormDto medicalFormDto, String invoiceId,
            List<MedicalPackageDTO> medicalPackageDTOs) {

        Set<MedicalPackageRep> medicalPackages = medicalPackageDTOs.stream()
                .map(dto -> new MedicalPackageRep(dto.medicalPackageId(), dto.name(), dto.price()))
                .collect(Collectors.toSet());

        BigDecimal totalAmount = medicalPackageDTOs.stream()
                .map(MedicalPackageDTO::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InvoiceProjection.builder()
                .id(invoiceId)
                .patientId(medicalFormDto.patientId())
                .totalAmount(totalAmount)
                .medicalPackages(medicalPackages)
                .status(InvoiceStatus.PENDING_PAYMENT)
                .build();
    }


    private void saveProjection(InvoiceProjection projection) {
        projection.markCreated();
        invoiceRepository.save(projection);
    }


}
