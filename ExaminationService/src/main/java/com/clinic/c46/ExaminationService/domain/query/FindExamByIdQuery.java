package com.clinic.c46.ExaminationService.domain.query;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FindExamByIdQuery {
    private String examId;
}
