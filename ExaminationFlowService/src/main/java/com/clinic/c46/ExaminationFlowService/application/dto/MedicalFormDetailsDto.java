package com.clinic.c46.ExaminationFlowService.application.dto;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import lombok.Builder;

import java.util.Optional;

@Builder
public record MedicalFormDetailsDto(String id, Optional<ExamDetailsDto> examination,
                                    String medicalFormStatus) {
}
