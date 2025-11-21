package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.medicalPackage.GetServiceByIdQuery;
import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.application.query.ExistsAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllServicesOfPackagesQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper.ServiceMapper;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.PackageRepView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.ServiceRepView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.ServiceRepViewRepository;
import lombok.RequiredArgsConstructor;
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
public class ServiceQueryHandler {
    private final QueryGateway queryGateway;
    private final ServiceRepViewRepository serviceRepViewRepository;
    private final ServiceMapper serviceMapper;

    @QueryHandler
    public List<ServiceRepDto> handle(GetAllServicesOfPackagesQuery query) {
        Set<String> packageIds = query.packageIds();
        Boolean allExist = queryGateway.query(ExistsAllPackageByIdsQuery.builder()
                        .packageIds(packageIds)
                        .build(), ResponseTypes.instanceOf(Boolean.class))
                .join();

        if (!allExist) {
            throw new ResourceNotFoundException(
                    "[MedicalFormServiceImpl] Medical Package(s) Not Found: " + packageIds.toString());
        }

        List<PackageRepView> packageRepViews = queryGateway.query(new GetAllPackageByIdsQuery(packageIds),
                        ResponseTypes.multipleInstancesOf(PackageRepView.class))
                .join();

        List<ServiceRepView> serviceRepViews = new ArrayList<>();

        for (PackageRepView packageRepView : packageRepViews) {
            serviceRepViews.addAll(packageRepView.getServices());
        }

        return serviceMapper.toDto(serviceRepViews);

    }

    @QueryHandler
    public Optional<ServiceRepDto> handle(GetServiceByIdQuery query) {
        return serviceRepViewRepository.findById(query.medicalServiceId())
                .map(serviceMapper::toDto);
    }
}
