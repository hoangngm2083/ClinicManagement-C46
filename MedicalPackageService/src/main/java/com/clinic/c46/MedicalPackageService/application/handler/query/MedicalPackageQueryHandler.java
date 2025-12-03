package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.CommonService.dto.MedicalPackageDTO;
import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import com.clinic.c46.CommonService.query.medicalPackage.FindMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesInIdsQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesQuery;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackageDetailDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackagesPagedDto;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDTO;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalPackageQueryHandler extends BaseQueryHandler {

    private final MedicalPackageViewRepository packageRepo;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;


    @QueryHandler
    public MedicalPackagesPagedDto handle(GetAllPackagesQuery q) {


        Pageable pageable = pageAndSortHelper.buildPageable(q.page(), q.size(), "price", q.sort());

        Specification<MedicalPackageView> spec = specificationBuilder.keyword(q.keyword(),
                List.of("name", "description"));

        Page<MedicalPackageView> pageResult = packageRepo.findAll(spec, pageable);


        return pageAndSortHelper.toPaged(pageResult, view -> MedicalPackageDTO.builder()
                .medicalPackageId(view.getId())
                .name(view.getName())
                .description(view.getDescription())
                .image(view.getImage())
                .price(view.getPrice())
                .build(), MedicalPackagesPagedDto::new);

    }

    @QueryHandler
    public MedicalPackageDetailDTO handle(FindMedicalPackageByIdQuery q) {

        return packageRepo.findById(q.medicalPackageId())
                .map(view -> MedicalPackageDetailDTO.builder()
                        .medicalPackageId(view.getId())
                        .price(view.getPrice())
                        .name(view.getName())
                        .image(view.getImage())
                        .description(view.getDescription())
                        .medicalServices(view.getMedicalServices()
                                .stream()
                                .map(serviceView -> MedicalServiceDTO.builder()
                                        .name(serviceView.getName())
                                        .medicalServiceId(serviceView.getId())
                                        .description(serviceView.getDescription())
                                        .departmentId(serviceView.getDepartmentId())
                                        .departmentName(serviceView.getDepartmentName())
                                        .processingPriority(serviceView.getProcessingPriority())
                                        .formTemplate(serviceView.getFormTemplate())
                                        .build())
                                .toList())
                        .build())
                .orElse(null);
    }

    @QueryHandler
    public Set<MedicalPackageDTO> handle(GetAllPackagesInIdsQuery q) {
        return packageRepo.findAllById(q.ids())
                .stream()
                .map(view -> MedicalPackageDTO.builder()
                        .medicalPackageId(view.getId())
                        .name(view.getName())
                        .description(view.getDescription())
                        .image(view.getImage())
                        .price(view.getPrice())
                        .build())
                .collect(Collectors.toSet());
    }
}
