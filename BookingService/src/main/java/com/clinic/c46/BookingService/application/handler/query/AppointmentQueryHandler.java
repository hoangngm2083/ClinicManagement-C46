package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.dto.AppointmentDetailsDto;
import com.clinic.c46.BookingService.application.dto.AppointmentDto;
import com.clinic.c46.BookingService.application.dto.ServiceDto;
import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.domain.enums.AppointmentState;
import com.clinic.c46.BookingService.domain.query.GetAppointmentByIdQuery;
import com.clinic.c46.BookingService.domain.query.SearchAppointmentsQuery;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.AppointmentsPagedResponse;
import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentQueryHandler {

    private final AppointmentViewRepository appointmentViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;

    @QueryHandler
    public AppointmentsPagedResponse handle(SearchAppointmentsQuery q) {

        int page = Math.max(q.getPage(), 0);
        Pageable pageable = pageAndSortHelper.buildPageable(page, q.getSortBy(), SortDirection.valueOf(q.getSort()));

        Specification<AppointmentView> specKeyword = specificationBuilder.keyword(q.getKeyword(),
                List.of("patientName"));

        String stateToSearch = q.getState() == null ? AppointmentState.CREATED.name() : q.getState();
        Specification<AppointmentView> specState = specificationBuilder.fieldEquals("state", stateToSearch);

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
                            .state(view.getState())
                            .createdAt(view.getCreatedAt())
                            .updatedAt(view.getUpdatedAt())
                            .services(services)
                            .build();
                });
    }


}


