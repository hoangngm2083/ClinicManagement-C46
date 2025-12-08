package com.clinic.c46.MedicalPackageService.application.dto;

import com.clinic.c46.CommonService.dto.BasePagedResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@NoArgsConstructor(force = true)
@SuperBuilder
public class MedicalServicesPagedDto extends BasePagedResponse<MedicalServiceDTO> {
}
