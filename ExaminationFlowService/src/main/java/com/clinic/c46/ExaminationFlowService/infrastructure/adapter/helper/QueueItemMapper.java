package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper;


import com.clinic.c46.ExaminationFlowService.application.dto.QueueItemDto;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.QueueItemView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface QueueItemMapper {

    @Mappings({@Mapping(source = "view.id", target = "queueItemId")})
    QueueItemDto toDto(QueueItemView view);

}

