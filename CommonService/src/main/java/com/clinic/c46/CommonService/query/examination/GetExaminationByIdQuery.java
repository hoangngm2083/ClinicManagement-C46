package com.clinic.c46.CommonService.query.examination;

import lombok.Builder;

@Builder
public record GetExaminationByIdQuery(String examinationId) {
}
