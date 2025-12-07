package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.dto.AppointmentDetailsDto;
import com.clinic.c46.BookingService.application.dto.AppointmentDto;
import com.clinic.c46.BookingService.application.dto.ServiceDto;
import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.domain.query.GetAppointmentByIdQuery;
import com.clinic.c46.BookingService.domain.query.SearchAppointmentsQuery;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.infrastructure.adapter.exception.DataNotFoundRetryableException;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.AppointmentsPagedResponse;
import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.appointment.GetAppointmentDetailsByIdQuery;
import com.clinic.c46.CommonService.query.appointment.GetAppointmentByPatientIdAndDateQuery;
import com.clinic.c46.CommonService.query.patient.GetPatientOptByIdQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentQueryHandler {

    private final AppointmentViewRepository appointmentViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;
    private final QueryGateway queryGateway;

    @QueryHandler
    public AppointmentsPagedResponse handle(SearchAppointmentsQuery q) {

        Pageable pageable = pageAndSortHelper.buildPageable(q.getPage(), q.getSize(), q.getSortBy(),
                SortDirection.valueOf(q.getSort()));

        Specification<AppointmentView> specKeyword = specificationBuilder.keyword(q.getKeyword(),
                List.of("patientName"));

        Specification<AppointmentView> specState = q.getState() != null ? specificationBuilder.fieldEquals("state",
                q.getState()) : null;

        Specification<AppointmentView> specDate = specificationBuilder.fromTo("date", LocalDate.class, q.getDateFrom(),
                q.getDateTo());

        Specification<AppointmentView> finalSpec = Specification.allOf(specKeyword)
                .and(specState)
                .and(specDate);

        Page<AppointmentView> pageResult = appointmentViewRepository.findAll(finalSpec, pageable);

        return pageAndSortHelper.toPaged(pageResult, view -> {
            MedicalPackageView medicalPackage = view.getMedicalPackage();


            return AppointmentDto.builder()
                    .id(view.getId())
                    .patientId(view.getPatientId())
                    .patientName(view.getPatientName())
                    .shift(view.getShift())
                    .date(view.getDate())
                    .medicalPackageId(medicalPackage.getMedicalPackageId())
                    .medicalPackageName(medicalPackage.getMedicalPackageName())
                    .snapshotPrice(view.getSnapshotPrice())
                    .snapshotPriceVersion(view.getSnapshotPriceVersion())
                    .state(view.getState())
                    .createdAt(view.getCreatedAt())
                    .updatedAt(view.getUpdatedAt())
                    .build();

        }, AppointmentsPagedResponse::new);
    }

    @QueryHandler
    public Optional<AppointmentDetailsDto> handle(GetAppointmentByIdQuery q) {
        return appointmentViewRepository.findById(q.appointmentId())
                .map(view -> {
                    MedicalPackageView medicalPackage = view.getMedicalPackage();
                    Set<ServiceDto> services = medicalPackage.getServices()
                            .stream()
                            .map(serviceRepView -> ServiceDto.builder()
                                    .id(serviceRepView.getId())
                                    .name(serviceRepView.getName())
                                    .build())
                            .collect(Collectors.toSet());

                    return AppointmentDetailsDto.builder()
                            .id(view.getId())
                            .patientId(view.getPatientId())
                            .patientName(view.getPatientName())
                            .shift(view.getShift())
                            .date(view.getDate())
                            .medicalPackageId(medicalPackage.getMedicalPackageId())
                            .medicalPackageName(medicalPackage.getMedicalPackageName())
                            .snapshotPrice(view.getSnapshotPrice())
                            .snapshotPriceVersion(view.getSnapshotPriceVersion())
                            .state(view.getState())
                            .createdAt(view.getCreatedAt())
                            .updatedAt(view.getUpdatedAt())
                            .services(services)
                            .build();
                });
    }

    @QueryHandler
    @Retryable(retryFor = DataNotFoundRetryableException.class, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(maxDelayExpression = "${retry.maxDelay}"))
    public CompletableFuture<Optional<com.clinic.c46.CommonService.dto.AppointmentDetailsDto>> handle(
            GetAppointmentDetailsByIdQuery q) {
        Optional<AppointmentView> viewOpt = appointmentViewRepository.findById(q.appointmentId());

        if (viewOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        AppointmentView view = viewOpt.get();
        MedicalPackageView medicalPackage = view.getMedicalPackage();
        Set<com.clinic.c46.CommonService.dto.AppointmentDetailsDto.ServiceDto> services = medicalPackage.getServices()
                .stream()
                .map(serviceRepView -> com.clinic.c46.CommonService.dto.AppointmentDetailsDto.ServiceDto.builder()
                        .id(serviceRepView.getId())
                        .name(serviceRepView.getName())
                        .build())
                .collect(Collectors.toSet());

        // Query patient email asynchronously
        GetPatientOptByIdQuery patientQuery = GetPatientOptByIdQuery.builder()
                .patientId(view.getPatientId())
                .build();

        return queryGateway.query(patientQuery, ResponseTypes.optionalInstanceOf(PatientDto.class))
                .thenApply(patientOpt -> {
                    if (patientOpt.isEmpty()) {
                        log.warn("[AppointmentQueryHandler] Patient not found: {}, will retry", view.getPatientId());
                        throw new DataNotFoundRetryableException("Patient not found: " + view.getPatientId());
                    }

                    String patientEmail = patientOpt.get()
                            .email();

                    com.clinic.c46.CommonService.dto.AppointmentDetailsDto dto = com.clinic.c46.CommonService.dto.AppointmentDetailsDto.builder()
                            .id(view.getId())
                            .patientId(view.getPatientId())
                            .patientName(view.getPatientName())
                            .patientEmail(patientEmail)
                            .shift(view.getShift())
                            .date(view.getDate())
                            .medicalPackageId(medicalPackage.getMedicalPackageId())
                            .medicalPackageName(medicalPackage.getMedicalPackageName())
                            .snapshotPrice(view.getSnapshotPrice())
                            .snapshotPriceVersion(view.getSnapshotPriceVersion())
                            .state(view.getState())
                            .services(services)
                            .build();

                    return Optional.of(dto);
                });
    }

    @Recover
    public CompletableFuture<Optional<com.clinic.c46.CommonService.dto.AppointmentDetailsDto>> recover(
            DataNotFoundRetryableException e, GetAppointmentDetailsByIdQuery q) {
        log.error(
                "[AppointmentQueryHandler] Failed to get appointment details after retries for appointmentId: {}, reason: {}",
                q.appointmentId(), e.getMessage(), e);

        // Return empty result when patient is not found even after retries
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @QueryHandler
    public Optional<com.clinic.c46.CommonService.dto.AppointmentDetailsDto> handle(GetAppointmentByPatientIdAndDateQuery q) {
        Specification<AppointmentView> specPatientId = specificationBuilder.fieldEquals("patientId", q.patientId());
        Specification<AppointmentView> specDate = specificationBuilder.fieldEquals("date", q.date());

        Specification<AppointmentView> finalSpec = Specification.allOf(specPatientId, specDate);

        return appointmentViewRepository.findAll(finalSpec).stream()
                .findFirst() // Get the first appointment if multiple exist
                .map(view -> {
                    MedicalPackageView medicalPackage = view.getMedicalPackage();
                    Set<com.clinic.c46.CommonService.dto.AppointmentDetailsDto.ServiceDto> services = medicalPackage.getServices()
                            .stream()
                            .map(serviceRepView -> com.clinic.c46.CommonService.dto.AppointmentDetailsDto.ServiceDto.builder()
                                    .id(serviceRepView.getId())
                                    .name(serviceRepView.getName())
                                    .build())
                            .collect(Collectors.toSet());

                    return com.clinic.c46.CommonService.dto.AppointmentDetailsDto.builder()
                            .id(view.getId())
                            .patientId(view.getPatientId())
                            .patientName(view.getPatientName())
                            .shift(view.getShift())
                            .date(view.getDate())
                            .medicalPackageId(medicalPackage.getMedicalPackageId())
                            .medicalPackageName(medicalPackage.getMedicalPackageName())
                            .snapshotPrice(view.getSnapshotPrice())
                            .snapshotPriceVersion(view.getSnapshotPriceVersion())
                            .state(view.getState())
                            .services(services)
                            .build();
                });
    }


}
