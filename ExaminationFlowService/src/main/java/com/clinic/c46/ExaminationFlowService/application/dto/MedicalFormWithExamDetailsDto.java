package com.clinic.c46.ExaminationFlowService.application.dto;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import lombok.Builder;

import java.util.Optional;

/**
 * Medical form DTO with examination details.
 * Used for EXAM_SERVICE type queue items.
 */
@Builder
public record MedicalFormWithExamDetailsDto(
        String id,
        Optional<ExamDetailsDto> examination,
        String medicalFormStatus) implements MedicalFormDetailsBase {
}
