package com.clinic.c46.CommonService.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum SortDirection {
    ASC("asc"), DESC("desc");
    private final String value;
}