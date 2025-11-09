package com.clinic.c46.CommonService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@SuperBuilder
public abstract class BasePagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long total;
    private int totalPages;
}
