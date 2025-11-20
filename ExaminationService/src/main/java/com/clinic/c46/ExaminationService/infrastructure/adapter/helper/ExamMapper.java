package com.clinic.c46.ExaminationService.infrastructure.adapter.helper;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.dto.MedicalResultDto;
import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.view.ExamView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.view.ResultView;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExamMapper {

    public ExamDetailsDto toExamDetailsDto(ExamView examView) {
        List<MedicalResultDto> resultDtos = examView.getResults().stream()
                .filter(r -> !r.getStatus().equals(ResultStatus.REMOVED))
                .map(this::toMedicalResultViewDto)
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

    private MedicalResultDto toMedicalResultViewDto(ResultView resultView) {
        return MedicalResultDto.builder()
                .doctorId(resultView.getDoctorId())
                .serviceId(resultView.getServiceId())
                .data(resultView.getData())
                .pdfUrl(resultView.getPdfUrl())
                .status(resultView.getStatus().name())
                .doctorName(resultView.getDoctorName())
                .build();
    }
}

