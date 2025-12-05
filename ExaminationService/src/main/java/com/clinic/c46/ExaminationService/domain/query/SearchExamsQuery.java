package com.clinic.c46.ExaminationService.domain.query;

import lombok.Builder;


@Builder
public record SearchExamsQuery(int page, int size, String keyword) {

}
