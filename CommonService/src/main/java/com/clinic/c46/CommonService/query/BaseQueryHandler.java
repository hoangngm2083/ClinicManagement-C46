package com.clinic.c46.CommonService.query;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;


@NoArgsConstructor
public abstract class BaseQueryHandler {
    @Value("${app.page-size}")
    protected int PAGE_SIZE = 10;

    protected int calcPage(int qPage) {
        return Math.max(qPage, 1) - 1;

    }
}
