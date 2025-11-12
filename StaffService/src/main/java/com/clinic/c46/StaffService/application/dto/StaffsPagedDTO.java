package com.clinic.c46.StaffService.application.dto;

import com.clinic.c46.CommonService.dto.BasePagedResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@NoArgsConstructor(force = true)
@SuperBuilder
public class StaffsPagedDTO extends BasePagedResponse<StaffDto> {
}
