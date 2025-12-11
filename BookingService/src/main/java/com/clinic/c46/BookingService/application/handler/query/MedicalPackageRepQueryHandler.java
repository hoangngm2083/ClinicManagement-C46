package com.clinic.c46.BookingService.application.handler.query;

import com.clinic.c46.BookingService.application.repository.MedicalPackageViewRepository;
import com.clinic.c46.BookingService.domain.query.ExistsMedicalPackageByIdQuery;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalPackageRepQueryHandler extends BaseQueryHandler {

    private final MedicalPackageViewRepository medicalPackageRepository;

    @QueryHandler
    public Boolean handle(ExistsMedicalPackageByIdQuery query) {
        return medicalPackageRepository.existsById(query.medicalPackageId());
    }
}
