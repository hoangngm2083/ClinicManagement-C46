package com.clinic.c46.StaffService.application.handler.query;


import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.staff.GetIdOfAllStaffQuery;
import com.clinic.c46.StaffService.application.dto.StaffDto;
import com.clinic.c46.StaffService.application.dto.StaffsPagedDTO;
import com.clinic.c46.StaffService.application.repository.DepartmentViewRepository;
import com.clinic.c46.StaffService.application.repository.StaffViewRepository;
import com.clinic.c46.StaffService.domain.enums.Role;
import com.clinic.c46.StaffService.domain.query.FindStaffByIdQuery;
import com.clinic.c46.StaffService.domain.query.FindStaffScheduleQuery;
import com.clinic.c46.StaffService.domain.query.GetAllStaffIdOfDepQuery;
import com.clinic.c46.StaffService.domain.query.GetAllStaffQuery;
import com.clinic.c46.StaffService.domain.view.StaffView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaffQueryHandler {

    private final StaffViewRepository staffViewRepository;
    private final DepartmentViewRepository departmentViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;

    @QueryHandler
    public StaffsPagedDTO handle(GetAllStaffQuery q) {

        Pageable pageable = pageAndSortHelper.buildPageable(q.page(), q.sortBy(), q.sort());

        Specification<StaffView> spec1 = specificationBuilder.keyword(q.keyword(), List.of("name", "description"));
        Specification<StaffView> spec2 = specificationBuilder.fieldEquals("departmentId", q.departmentId());
        Specification<StaffView> spec3 = specificationBuilder.fieldEquals("role", q.role() == null ? null : Role.findByCode(q.role()));
        Specification<StaffView> spec4 = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        Specification<StaffView> finalSpec = Specification.allOf(spec1)
                .and(spec2)
                .and(spec3)
                .and(spec4);

        Page<StaffView> pageResult = staffViewRepository.findAll(finalSpec, pageable);

        return pageAndSortHelper.toPaged(pageResult, view -> {
            String deptName = getDepartmentName(view.getDepartmentId());
            return StaffDto.builder()
                    .name(view.getName())
                    .description(view.getDescription())
                    .image(view.getImage())
                    .id(view.getId())
                    .email(view.getEmail())
                    .eSignature(view.getESignature())
                    .departmentId(view.getDepartmentId())
                    .departmentName(deptName)
                    .phone(view.getPhone())
                    .role(view.getRole()
                            .getCode())
                    .build();
        }, StaffsPagedDTO::new);
    }

    @QueryHandler
    public Optional<StaffDto> handle(FindStaffByIdQuery query) {
        log.info("Handling FindStaffByIdQuery for staffId: {}", query.staffId());

        return staffViewRepository.findById(query.staffId())
                .filter(staff -> !staff.isDeleted())
                .map(this::toStaffDto);
    }

    @QueryHandler
    public List<StaffDto> handle(FindStaffScheduleQuery query) {
        log.info("Handling FindStaffScheduleQuery for month: {}, year: {}", query.month(), query.year());

        // Validate month and year
        validateMonthAndYear(query.month(), query.year());


        LocalDate start = LocalDate.of(query.year(), query.month(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<StaffView> activeStaff = staffViewRepository.findStaffWithDayOffsBetween(start, end);

        // Filter staff based on schedule logic and exclude deleted staff
        return activeStaff.stream()
                .filter(staff -> !staff.isDeleted())
                .map(this::toStaffDto)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public List<String> handle(GetIdOfAllStaffQuery query) {
        return staffViewRepository.findAll()
                .stream()
                .map(StaffView::getId)
                .toList();
    }

    @QueryHandler
    public List<String> handle(GetAllStaffIdOfDepQuery query) {
        return staffViewRepository.findAllByDepartmentId(query.departmentId())
                .stream()
                .filter(staff -> !staff.isDeleted())
                .map(StaffView::getId)
                .toList();
    }

    private StaffDto toStaffDto(StaffView staffView) {
        String deptName = getDepartmentName(staffView.getDepartmentId());
        return new StaffDto(staffView.getId(), staffView.getName(), staffView.getEmail(), staffView.getPhone(),
                staffView.getDescription(), staffView.getImage(), staffView.getRole()
                .getCode(), staffView.getESignature(), staffView.getDepartmentId(), deptName);
    }

    private String getDepartmentName(String departmentId) {
        if (departmentId == null) return null;
        return departmentViewRepository.findById(departmentId)
                .map(com.clinic.c46.StaffService.domain.view.DepartmentView::getName)
                .orElse(null);
    }


    private void validateMonthAndYear(int month, int year) {
        try {
            YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid month/year combination: " + month + "/" + year);
        }
    }
}