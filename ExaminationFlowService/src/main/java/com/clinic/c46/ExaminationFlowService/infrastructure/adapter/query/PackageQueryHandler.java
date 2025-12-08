package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.ExaminationFlowService.application.dto.PackageRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.ExistsAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.ServiceMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.PackageRepView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.PackageRepViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PackageQueryHandler {

    private final PackageRepViewRepository packageRepViewRepository;
    private final ServiceMapper serviceMapper;
    private final SpecificationBuilder specificationBuilder;


    @QueryHandler
    public boolean handle(ExistsAllPackageByIdsQuery query) {
        if (query.packageIds()
                .isEmpty()) {
            return false;
        }
        long existingCount = packageRepViewRepository.countByIdIn(query.packageIds());
        return existingCount == query.packageIds()
                .size();
    }

    @QueryHandler
    public List<PackageRepDto> handle(GetAllPackageByIdsQuery query) {
        log.warn("+++++++++++ [PackageQueryHandler] Received GetAllPackageByIdsQuery for package IDs: {}",
                query.packageIds());
        if (query.packageIds()
                .isEmpty()) {
//            return new ArrayList<>();
            return List.of();

        }
        log.warn("+++++++++++ [PackageQueryHandler] Fetching PackageRepViews for package IDs: {}", query.packageIds());

        Specification<PackageRepView> inSpec = specificationBuilder.in("id", query.packageIds()
                .stream()
                .toList());

        Specification<PackageRepView> notDeletedSpec = specificationBuilder.notDeleted();

        Specification<PackageRepView> finalSpec = Specification.allOf(inSpec, notDeletedSpec);

        List<PackageRepView> medicalPackages = packageRepViewRepository.findAll(finalSpec);

        if (medicalPackages.isEmpty()) {
            log.warn("+++++++++++ [PackageQueryHandler] No PackageRepViews found for package IDs: {}",
                    query.packageIds());
            return List.of();
        }

        log.warn("+++++++++++ [PackageQueryHandler] Retrieved PackageRepViews: {}", medicalPackages.toString());
        return medicalPackages.stream()
                .map(p -> {
                    BigDecimal currentPrice = p.getCurrentPrice();
                    return new PackageRepDto(p.getId(), currentPrice, p.getCurrentPriceVersion(), p.getServices()
                            .stream()
                            .map(serviceMapper::toDto)
                            .collect(Collectors.toSet()));
                })
                .toList();

    }


}
