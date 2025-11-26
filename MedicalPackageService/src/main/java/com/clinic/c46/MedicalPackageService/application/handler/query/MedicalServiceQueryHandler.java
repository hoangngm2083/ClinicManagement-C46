package com.clinic.c46.MedicalPackageService.application.handler.query;

import com.clinic.c46.CommonService.query.BaseQueryHandler;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceDTO;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServicesPagedDto;
import com.clinic.c46.MedicalPackageService.application.repository.MedicalServiceViewRepository;
import com.clinic.c46.MedicalPackageService.domain.query.GetAllMedicalServicesQuery;
import com.clinic.c46.MedicalPackageService.domain.query.GetMedicalServiceByIdQuery;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.domain.view.MedicalServiceView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicalServiceQueryHandler extends BaseQueryHandler {

        private final MedicalServiceViewRepository serviceRepo;

        @QueryHandler
        public MedicalServicesPagedDto handle(GetAllMedicalServicesQuery query) {

                int page = this.calcPage(query.page());
                Pageable pageable = PageRequest.of(page, PAGE_SIZE);

                Specification<MedicalServiceView> spec = Specification.allOf();

                String keyword = query.keyword();
                if (keyword != null && !keyword.isBlank()) {
                        String lowerKeyword = "%" + keyword.toLowerCase() + "%";

                        spec = spec.and((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("name")), lowerKeyword),
                                        cb.like(cb.lower(root.get("description")), lowerKeyword),
                                        cb.like(cb.lower(root.get("departmentName")), lowerKeyword)));
                }
                Page<MedicalServiceView> pageResult = serviceRepo.findAll(spec, pageable);

                List<MedicalServiceDTO> content = pageResult.map(view -> MedicalServiceDTO.builder()
                                .medicalServiceId(view.getId())
                                .name(view.getName())
                                .departmentId(view.getDepartmentId())
                                .departmentName(view.getDepartmentName())
                                .processingPriority(view.getProcessingPriority())
                                .formTemplate(view.getFormTemplate())
                                .description(view.getDescription())
                                .build())
                                .toList();

                return MedicalServicesPagedDto.builder()
                                .content(content)
                                .page(page)
                                .size(content.size())
                                .total(pageResult.getTotalElements())
                                .totalPages(pageResult.getTotalPages())
                                .build();
        }

        @QueryHandler
        public Optional<MedicalServiceDTO> handle(GetMedicalServiceByIdQuery q) {
                return serviceRepo.findById(q.medicalServiceId())
                                .map(view -> MedicalServiceDTO.builder()
                                                .medicalServiceId(view.getId())
                                                .name(view.getName())
                                                .processingPriority(view.getProcessingPriority())
                                                .description(view.getDescription())
                                                .formTemplate(view.getFormTemplate())
                                                .build());
        }

        @QueryHandler
        public Boolean handle(ExistsServiceByIdQuery query) {
                return serviceRepo.existsById(query.serviceId());
        }

}
