package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.repository.AppointmentViewRepository;
import com.clinic.c46.BookingService.domain.query.SearchAppointmentsQuery;
import com.clinic.c46.BookingService.domain.view.AppointmentView;
import com.clinic.c46.BookingService.domain.view.MedicalPackageView;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.AppointmentDto;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentQueryHandler {

    private final AppointmentViewRepository appointmentViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;

    @QueryHandler
    public AppointmentsPagedResponse handle(SearchAppointmentsQuery q) {

        int page = Math.max(q.page(), 0);
        Pageable pageable = pageAndSortHelper.buildPageable(page, q.sortBy(), SortDirection.valueOf(q.sort()));

        Specification<AppointmentView> specKeyword = specificationBuilder.keyword(q.keyword(), List.of("patientName"));

        String stateToSearch = q.state() == null ? "CREATED" : q.state();
        Specification<AppointmentView> specState = specificationBuilder.fieldEquals("state", stateToSearch);

        Specification<AppointmentView> specDate = specificationBuilder.fromTo("date", LocalDate.class, q.dateFrom(),
                q.dateTo());

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

}
