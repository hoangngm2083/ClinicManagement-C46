package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.MedicalPackageService.application.repository.BulkImportStatusRepository;
import com.clinic.c46.MedicalPackageService.domain.query.GetBulkImportStatusQuery;
import com.clinic.c46.MedicalPackageService.domain.view.BulkImportStatusView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BulkImportStatusQueryHandler {

    private final BulkImportStatusRepository repository;

    @QueryHandler
    public BulkImportStatusView handle(GetBulkImportStatusQuery query) {
        return repository.findById(query.bulkId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bulk import status not found for bulkId: " + query.bulkId()));
    }
}
