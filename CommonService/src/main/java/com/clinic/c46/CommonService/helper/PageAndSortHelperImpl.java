package com.clinic.c46.CommonService.helper;

import com.clinic.c46.CommonService.dto.BasePagedResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.function.Supplier;


@Service
public class PageAndSortHelperImpl implements PageAndSortHelper {
    @Value("${app.page-size:10}")
    private int defaultPageSize;

    @Override
    public Pageable buildPageable(int page, String sortField, SortDirection sortDir) {
        int safePage = Math.max(page - 1, 0);
        Sort sort = buildSort(sortField, sortDir);
        return PageRequest.of(safePage, defaultPageSize, sort);
    }

    @Override
    public <T, U, R extends BasePagedResponse<U>> R toPaged(
            Page<T> page,
            Function<T, U> mapper,
            Supplier<R> responseSupplier
                                                           ) {
        R response = responseSupplier.get();
        response.setContent(page.map(mapper)
                .toList());
        response.setPage(page.getNumber() + 1);
        response.setSize(page.getSize());
        response.setTotal(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Override
    public Sort buildSort(String sortField, SortDirection sortDir) {
        if (sortField == null || sortField.isBlank()) {
            sortField = "createdAt";
        }
        if (sortDir == null) {
            sortDir = SortDirection.ASC;
        }

        return sortDir.equals(SortDirection.DESC) ? Sort.by(sortField)
                .descending() : Sort.by(sortField)
                .ascending();
    }
}
