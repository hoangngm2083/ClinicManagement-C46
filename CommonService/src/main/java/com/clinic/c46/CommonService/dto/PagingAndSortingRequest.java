package com.clinic.c46.CommonService.dto;

import com.clinic.c46.CommonService.helper.SortDirection;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagingAndSortingRequest {
    private int page = 1;
    private String keyword;
    private String sortField = "price";
    private SortDirection sortDirection = SortDirection.ASC;
}

