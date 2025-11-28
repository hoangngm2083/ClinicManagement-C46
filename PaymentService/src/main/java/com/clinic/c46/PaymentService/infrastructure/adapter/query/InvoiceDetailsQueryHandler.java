package com.clinic.c46.PaymentService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
import com.clinic.c46.CommonService.dto.MedicalPackageRepDto;
import com.clinic.c46.CommonService.query.invoice.GetInvoiceDetailsByIdQuery;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.InvoiceProjection;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.projection.MedicalPackageRep;
import com.clinic.c46.PaymentService.infrastructure.adapter.persistence.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceDetailsQueryHandler {

        private final InvoiceRepository invoiceRepository;
        private final QueryGateway queryGateway;

        @QueryHandler
        public CompletableFuture<Optional<InvoiceDetailsDto>> handle(GetInvoiceDetailsByIdQuery query) {
                log.info("[InvoiceDetailsQueryHandler.handle] START: Getting invoice details for invoiceId={}",
                                query.invoiceId());

                Optional<InvoiceProjection> invoiceOptional = invoiceRepository.findById(query.invoiceId());

                if (invoiceOptional.isEmpty()) {
                        log.warn("[InvoiceDetailsQueryHandler.handle] Invoice NOT found for invoiceId={}",
                                        query.invoiceId());
                        return CompletableFuture.completedFuture(Optional.empty());
                }

                InvoiceProjection invoice = invoiceOptional.get();
                log.info(
                                "[InvoiceDetailsQueryHandler.handle] Invoice found: id={}, patientId={}, status={}, totalAmount={}",
                                invoice.getId(), invoice.getPatientId(), invoice.getStatus(), invoice.getTotalAmount());

                // Query patient details
                GetPatientByIdQuery getPatientByIdQuery = GetPatientByIdQuery.builder()
                                .patientId(invoice.getPatientId())
                                .build();

                log.debug("[InvoiceDetailsQueryHandler.handle] Querying Patient: patientId={}", invoice.getPatientId());
                CompletableFuture<PatientDto> patientFuture = queryGateway.query(getPatientByIdQuery,
                                ResponseTypes.instanceOf(PatientDto.class))
                                .handle((patientDto, throwable) -> {
                                        if (throwable != null) {
                                                log.warn(
                                                                "[InvoiceDetailsQueryHandler.handle] FAILED to retrieve Patient data for patientId={}, invoice={}: {}",
                                                                invoice.getPatientId(), invoice.getId(),
                                                                throwable.getMessage(), throwable);
                                                return null;
                                        }
                                        if (patientDto == null) {
                                                log.warn(
                                                                "[InvoiceDetailsQueryHandler.handle] Patient is NULL from query gateway for patientId={}, invoice={}",
                                                                invoice.getPatientId(), invoice.getId());
                                                return null;
                                        }
                                        log.info(
                                                        "[InvoiceDetailsQueryHandler.handle] Patient retrieved successfully: patientId={}, name={}",
                                                        patientDto.patientId(), patientDto.name());
                                        return patientDto;
                                });

                return patientFuture.thenApply(patientDto -> {
                        // Convert MedicalPackageRep to MedicalPackageRepDto
                        Set<MedicalPackageRepDto> medicalPackageDtos = invoice.getMedicalPackages().stream()
                                        .map(this::toMedicalPackageRepDto)
                                        .collect(Collectors.toSet());

                        InvoiceDetailsDto invoiceDetails = InvoiceDetailsDto.builder()
                                        .invoiceId(invoice.getId())
                                        .patientId(invoice.getPatientId())
                                        .patientName(patientDto != null ? patientDto.name() : null)
                                        .patientEmail(patientDto != null ? patientDto.email() : null)
                                        .patientPhone(patientDto != null ? patientDto.phone() : null)
                                        .medicalPackages(medicalPackageDtos)
                                        .totalAmount(invoice.getTotalAmount())
                                        .status(invoice.getStatus().toString())
                                        .build();

                        log.info("[InvoiceDetailsQueryHandler.handle] SUCCESS: Returning invoice details for invoiceId={}",
                                        query.invoiceId());
                        return Optional.of(invoiceDetails);
                });
        }

        private MedicalPackageRepDto toMedicalPackageRepDto(MedicalPackageRep rep) {
                return MedicalPackageRepDto.builder()
                                .id(rep.id())
                                .name(rep.name())
                                .price(rep.price())
                                .build();
        }
}
