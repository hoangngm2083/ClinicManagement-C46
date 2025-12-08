package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDetailsDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServicesPagedDto;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.query.GetAllMedicalServicesQuery;
import com.clinic.c46.MedicalPackageService.domain.query.GetMedicalServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalServiceQueryHandler extends BaseQueryHandler {

    private final MedicalServiceViewRepository serviceRepo;
    private final MedicalPackageViewRepository packageRepo;
    private final SpecificationBuilder specBuilder;
    private final PageAndSortHelper pageHelper;

    @QueryHandler
    public MedicalServicesPagedDto handle(GetAllMedicalServicesQuery query) {
        var pageable = pageHelper.buildPageable(query.page(), PAGE_SIZE, null, null);

        Specification<MedicalServiceView> spec = specBuilder.notDeleted();

        String medicalPackageId = query.medicalPackageId();
        if (medicalPackageId != null && !medicalPackageId.isBlank()) {
            // Get the medical package to find its associated service IDs
            var medicalPackage = packageRepo.findById(medicalPackageId);
            if (medicalPackage.isPresent()) {
                var serviceIds = medicalPackage.get()
                        .getMedicalServices()
                        .stream()
                        .map(MedicalServiceView::getId)
                        .toList();
                spec = spec.and(specBuilder.in("id", serviceIds));
            } else {
                // If package doesn't exist, return empty result
                spec = spec.and(specBuilder.in("id", List.of()));
            }
        }

        spec = spec.and(specBuilder.keyword(query.keyword(), List.of("name", "description", "departmentName")));

        Page<MedicalServiceView> pageResult = serviceRepo.findAll(spec, pageable);

        return pageHelper.toPaged(pageResult, view -> MedicalServiceDTO.builder()
                .medicalServiceId(view.getId())
                .name(view.getName())
                .departmentId(view.getDepartmentId())
                .departmentName(view.getDepartmentName())
                .processingPriority(view.getProcessingPriority())
                .description(view.getDescription())
                .build(), MedicalServicesPagedDto::new);
    }

    @QueryHandler
    public Optional<MedicalServiceDetailsDTO> handle(GetMedicalServiceByIdQuery q) {
        return serviceRepo.findById(q.medicalServiceId())
                .map(view -> MedicalServiceDetailsDTO.builder()
                        .medicalServiceId(view.getId())
                        .name(view.getName())
                        .processingPriority(view.getProcessingPriority())
                        .description(view.getDescription())
                        .departmentId(view.getDepartmentId())
                        .departmentName(view.getDepartmentName())
                        .formTemplate(view.getFormTemplate())
                        .build());
    }

    @QueryHandler
    public Boolean handle(ExistsServiceByIdQuery query) {
        return serviceRepo.existsById(query.serviceId());
    }

}
