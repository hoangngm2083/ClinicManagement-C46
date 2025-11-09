package com.clinic.c46.StaffService.application.handler.query;


import com.clinic.c46.CommonService.query.BaseQueryHandler;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentQueryHandler extends BaseQueryHandler {

    private final DepartmentViewRepository departmentViewRepository;


    @QueryHandler
    public DepartmentsPagedDTO handle(GetAllDepartmentsQuery query) {

        int page = this.calcPage(query.page());
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Specification<DepartmentView> spec = Specification.allOf();

        String keyword = query.keyword();
        if (keyword != null && !keyword.trim()
                .isBlank()) {
            String lowerKeyword = "%" + keyword.trim()
                    .toLowerCase() + "%";

            spec = spec.and((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("name")), lowerKeyword),
                    cb.like(cb.lower(root.get("description")), lowerKeyword)));
        }


        Page<DepartmentView> pageResult = departmentViewRepository.findAll(spec, pageable);

        List<DepartmentDTO> content = pageResult.map(view -> DepartmentDTO.builder()
                        .id(view.getId())
                        .name(view.getName())
                        .description(view.getDescription())
                        .build())
                .toList();

        return DepartmentsPagedDTO.builder()
                .content(content)
                .page(page)
                .size(pageResult.getSize())
                .total(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
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


}
