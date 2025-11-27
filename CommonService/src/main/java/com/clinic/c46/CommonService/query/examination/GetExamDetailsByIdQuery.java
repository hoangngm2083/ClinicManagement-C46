package com.clinic.c46.CommonService.query.examination;

import lombok.Builder;

@Builder
public record GetExamDetailsByIdQuery(String examinationId) {
}
