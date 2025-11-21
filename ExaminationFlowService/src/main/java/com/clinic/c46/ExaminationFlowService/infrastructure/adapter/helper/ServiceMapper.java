package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper;

import com.clinic.c46.ExaminationFlowService.application.dto.ServiceRepDto;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.ServiceRepView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    @Mappings({@Mapping(source = "view.id", target = "serviceId")})
    ServiceRepDto toDto(ServiceRepView view);

    List<ServiceRepDto> toDto(List<ServiceRepView> views);
}
