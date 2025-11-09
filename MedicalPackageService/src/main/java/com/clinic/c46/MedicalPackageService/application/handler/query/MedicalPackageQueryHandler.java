package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.CommonService.dto.MedicalPackageDTO;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import com.clinic.c46.CommonService.query.medicalPackage.FindMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesQuery;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackagesPagedDto;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalPackageQueryHandler extends BaseQueryHandler {

    private final MedicalPackageViewRepository packageRepo;

    @QueryHandler
    public MedicalPackagesPagedDto handle(GetAllPackagesQuery q) {

        int page = this.calcPage(q.page());
        Pageable pageable = PageRequest.of( page, PAGE_SIZE);


        Specification<MedicalPackageView> spec = Specification.allOf();

        String keyword = q.keyword();
        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = "%" + keyword.toLowerCase() + "%";

            spec = spec.and((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("name")), lowerKeyword),
                    cb.like(cb.lower(root.get("description")), lowerKeyword),
                    cb.like(cb.lower(root.get("name")), lowerKeyword)));
        }
        Page<MedicalPackageView> pageResult = packageRepo.findAll(spec, pageable);

        List<MedicalPackageDTO> content = pageResult.map(view -> MedicalPackageDTO.builder()
                        .medicalPackageId(view.getId())
                        .name(view.getName())
                        .description(view.getDescription())
                        .price(view.getPrice())
                        .build())
                .toList();

        return MedicalPackagesPagedDto.builder()
                .content(content)
                .page(page)
                .size(content.size())
                .total(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @QueryHandler
    public MedicalPackageDTO handle(FindMedicalPackageByIdQuery q) {
        return packageRepo.findById(q.medicalPackageId())
                .map(view -> MedicalPackageDTO.builder()
                        .medicalPackageId(view.getId())
                        .price(view.getPrice())
                        .name(view.getName())
                        .description(view.getDescription())
                        .build())
                .orElse(null);
    }
}
