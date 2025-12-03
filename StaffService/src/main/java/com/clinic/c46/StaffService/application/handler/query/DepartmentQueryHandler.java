package com.clinic.c46.StaffService.application.handler.query;


import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import com.clinic.c46.CommonService.query.staff.GetIdOfAllDepartmentQuery;
import com.clinic.c46.StaffService.application.dto.DepartmentDTO;
import com.clinic.c46.StaffService.application.dto.DepartmentsPagedDTO;
import com.clinic.c46.StaffService.application.repository.DepartmentViewRepository;
import com.clinic.c46.StaffService.domain.query.GetAllDepartmentsQuery;
import com.clinic.c46.StaffService.domain.query.GetDepartmentByIdQuery;
import com.clinic.c46.StaffService.domain.view.DepartmentView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentQueryHandler extends BaseQueryHandler {

    private final DepartmentViewRepository departmentViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;


    @QueryHandler
    public DepartmentsPagedDTO handle(GetAllDepartmentsQuery query) {

        Pageable pageable = pageAndSortHelper.buildPageable(query.page(), query.size(), "", SortDirection.ASC);
        Specification<DepartmentView> spec = specificationBuilder.keyword(query.keyword(),
                List.of("name", "description"));

        
        Page<DepartmentView> pageResult = departmentViewRepository.findAll(spec, pageable);

        return pageAndSortHelper.toPaged(pageResult, view -> DepartmentDTO.builder()
                .id(view.getId())
                .name(view.getName())
                .description(view.getDescription())
                .build(), DepartmentsPagedDTO::new);
    }

    @QueryHandler
    public DepartmentDTO handle(GetDepartmentByIdQuery query) {
        return departmentViewRepository.findById(query.departmentId())
                .map(departmentView -> DepartmentDTO.builder()
                        .id(departmentView.getId())
                        .description(departmentView.getDescription())
                        .name(departmentView.getName())
                        .build())
                .orElse(null);
    }

    @QueryHandler
    public List<String> handle(GetIdOfAllDepartmentQuery query) {
        return departmentViewRepository.findAll()
                .stream()
                .map(DepartmentView::getId)
                .toList();
    }


}
