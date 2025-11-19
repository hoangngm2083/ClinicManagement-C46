package com.clinic.c46.ExaminationFlowService.application.dto;

import com.clinic.c46.CommonService.dto.ExaminationDto;
import com.clinic.c46.CommonService.dto.PatientDto;
import lombok.Builder;

import java.util.Optional;

@Builder
public record MedicalFormDetailsDto(String id, Optional<PatientDto> patient, Optional<ExaminationDto> examination,
                                    String medicalFormStatus) {
}
