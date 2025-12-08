package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.CommonService.domain.MedicalPackagePrice;
import com.clinic.c46.CommonService.dto.MedicalPackageDTO;
import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsAllServicesByIdsQuery;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.FindMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesInIdsQuery;
import com.clinic.c46.CommonService.query.medicalPackage.GetAllPackagesQuery;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackageDetailDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalPackagesPagedDto;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDetailsDTO;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalPackageView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalPackageQueryHandler extends BaseQueryHandler {

    private final MedicalPackageViewRepository packageRepo;
    private final MedicalServiceViewRepository serviceRepo;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;


    @QueryHandler
    public MedicalPackagesPagedDto handle(GetAllPackagesQuery q) {


        Pageable pageable = pageAndSortHelper.buildPageable(q.page(), q.size(), q.sortBy(), q.sort());

        Specification<MedicalPackageView> searchSpec = specificationBuilder.keyword(q.keyword(),
                List.of("name", "description"));

        Specification<MedicalPackageView> notDeletedSpec = specificationBuilder.notDeleted();

        Specification<MedicalPackageView> finalSpec = Specification.allOf(searchSpec, notDeletedSpec);

        Page<MedicalPackageView> pageResult = packageRepo.findAll(finalSpec, pageable);


        return pageAndSortHelper.toPaged(pageResult, view -> {
            BigDecimal currentPrice = view.getCurrentPrice();
            return MedicalPackageDTO.builder()
                    .medicalPackageId(view.getId())
                    .name(view.getName())
                    .description(view.getDescription())
                    .image(view.getImage())
                    .price(currentPrice)
                    .priceVersion(view.getCurrentPriceVersion())
                    .build();
        }, MedicalPackagesPagedDto::new);

    }

    @QueryHandler
    public MedicalPackageDetailDTO handle(FindMedicalPackageByIdQuery q) {

        return packageRepo.findById(q.medicalPackageId())
                .map(view -> MedicalPackageDetailDTO.builder()
                        .medicalPackageId(view.getId())
                        .prices(view.getPrices() != null ?
                                view.getPrices().stream()
                                        .collect(java.util.stream.Collectors.toMap(
                                                MedicalPackagePrice::getVersion,
                                                MedicalPackagePrice::getPrice
                                        )) : new HashMap<>())
                        .name(view.getName())
                        .image(view.getImage())
                        .description(view.getDescription())
                        .medicalServices(view.getMedicalServices()
                                .stream()
                                .map(serviceView -> MedicalServiceDetailsDTO.builder()
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
                .map(view -> {
                    BigDecimal currentPrice = view.getCurrentPrice();
                    return MedicalPackageDTO.builder()
                            .medicalPackageId(view.getId())
                            .name(view.getName())
                            .description(view.getDescription())
                            .image(view.getImage())
                            .price(currentPrice)
                            .priceVersion(view.getCurrentPriceVersion())
                            .build();
                })
                .collect(Collectors.toSet());
    }

    @QueryHandler
    public Boolean handle(ExistsMedicalPackageByIdQuery query) {
        return packageRepo.existsById(query.medicalPackageId());
    }

    @QueryHandler
    public Boolean handle(ExistsAllServicesByIdsQuery query) {
        if (query.serviceIds() == null || query.serviceIds().isEmpty()) {
            return true;
        }
        // Find all services by IDs and check if all exist (not deleted)
        long foundCount = serviceRepo.findAll(specificationBuilder.in("id", List.copyOf(query.serviceIds())))
                .stream()
                .filter(service -> !service.isDeleted())
                .count();
        return query.serviceIds().size() == foundCount;
    }
}
