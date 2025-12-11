package com.clinic.c46.CommonService.helper;

import com.clinic.c46.CommonService.dto.BasePagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;
import java.util.function.Supplier;


public interface PageAndSortHelper {
    Sort buildSort(String sortField, SortDirection sortDir);

    Pageable buildPageable(int page, int size, String sortField, SortDirection sortDir);

    <T, U, R extends BasePagedResponse<U>> R toPaged(Page<T> page, Function<T, U> mapper, Supplier<R> responseSupplier);
}
