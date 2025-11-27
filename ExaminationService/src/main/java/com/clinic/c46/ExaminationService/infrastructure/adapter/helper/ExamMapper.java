package com.clinic.c46.ExaminationService.infrastructure.adapter.helper;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.dto.MedicalResultDto;
import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ExamView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ResultView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ServiceRepView;
import org.springframework.stereotype.Component;

import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository.ServiceRepViewRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExamMapper {
    private final ServiceRepViewRepository serviceRepViewRepository;

    public ExamDetailsDto toExamDetailsDto(ExamView examView) {
        // Collect all service IDs from results
        var serviceIds = examView.getResults().stream()
                .map(r -> r.getServiceId())
                .distinct()
                .toList();
        
        // Fetch all service views in one query
        Map<String, ServiceRepView> serviceRepViews = serviceRepViewRepository.findAllById(serviceIds)
                .stream()
                .collect(Collectors.toMap(
                        ServiceRepView::getId,
                        s -> s
                ));
        
        List<MedicalResultDto> resultDtos = examView.getResults().stream()
                .filter(r -> !r.getStatus().equals(ResultStatus.REMOVED))
                .map(r -> toMedicalResultViewDto(r, serviceRepViews.get(r.getServiceId())))
                .toList();

        return ExamDetailsDto.builder()
                .id(examView.getId())
                .patientId(examView.getPatientId())
                .patientName(examView.getPatientName())
                .patientEmail(examView.getPatientEmail())
                .medicalFormId(examView.getMedicalFormId())
                .results(resultDtos)
                .build();
    }

    private MedicalResultDto toMedicalResultViewDto(ResultView resultView, ServiceRepView serviceRepView) {
        return MedicalResultDto.builder()
                .doctorId(resultView.getDoctorId())
                .serviceId(resultView.getServiceId())
                .serviceName(serviceRepView != null ? serviceRepView.getName() : null)
                .data(resultView.getData())
                .pdfUrl(resultView.getPdfUrl())
                .status(resultView.getStatus().name())
                .doctorName(resultView.getDoctorName())
                .serviceFormTemplate(serviceRepView != null ? serviceRepView.getFormTemplate() : null)
                .build();
    }
}

