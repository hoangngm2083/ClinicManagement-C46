package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.query;

import com.clinic.c46.ExaminationFlowService.application.query.ExistsAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.application.query.GetAllPackageByIdsQuery;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.PackageRepView;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.repository.PackageRepViewRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PackageQueryHandler {

    private final PackageRepViewRepository packageRepViewRepository;


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
    public List<PackageRepView> handle(GetAllPackageByIdsQuery query) {
        if (query.packageIds()
                .isEmpty()) {
            return new ArrayList<>();
        }
        return packageRepViewRepository.findAllById(query.packageIds());

    }


}
