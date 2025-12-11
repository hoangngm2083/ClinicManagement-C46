package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.medicalPackage.GetServiceByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.PackageRepDto;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.ExistsAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllServicesOfPackagesQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.ServiceMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.ServiceRepViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceQueryHandler {
    private final QueryGateway queryGateway;
    private final ServiceRepViewRepository serviceRepViewRepository;
    private final ServiceMapper serviceMapper;
    private final SpecificationBuilder specificationBuilder;

    @QueryHandler
    public List<ServiceRepDto> handle(GetAllServicesOfPackagesQuery query) {

        log.warn("+++++++++++ [ServiceQueryHandler] Received GetAllServicesOfPackagesQuery for package IDs: {}",
                query.packageIds());

        Set<String> packageIds = query.packageIds();
        Boolean allExist = queryGateway.query(ExistsAllPackageByIdsQuery.builder()
                        .packageIds(packageIds)
                        .build(), ResponseTypes.instanceOf(Boolean.class))
                .join();

        log.warn("+++++++++++ [ServiceQueryHandler] Existence check for package IDs {}: {}", packageIds, allExist);

        if (!allExist) {
            throw new ResourceNotFoundException(
                    "[MedicalFormServiceImpl] Medical Package(s) Not Found: " + packageIds.toString());
        }

        List<PackageRepDto> packageDtos = queryGateway.query(new GetAllPackageByIdsQuery(packageIds),
                        ResponseTypes.multipleInstancesOf(PackageRepDto.class))
                .join();

        log.warn("+++++++++++ [ServiceQueryHandler] Retrieved PackageRepViews: {}", packageDtos);
        List<ServiceRepDto> serviceRepDtos = new ArrayList<>();

        for (PackageRepDto packageDto : packageDtos) {
            serviceRepDtos.addAll(packageDto.services());
        }

        return serviceRepDtos;

    }

    @QueryHandler
    public Optional<ServiceRepDto> handle(GetServiceByIdQuery query) {
        return serviceRepViewRepository.findById(query.medicalServiceId())
                .map(serviceMapper::toDto);
    }
}
