package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.helper;


import com.clinic.c46.ExaminationFlowService.application.dto.MedicalFormDto;
import com.clinic.c46.ExaminationFlowService.application.service.medicalForm.dto.CreateMedicalFormDto;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.controller.dto.CreateMedicalFormRequest;
import com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection.MedicalFormView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MedicalFormMapper {

    CreateMedicalFormDto toDto(CreateMedicalFormRequest request);

    MedicalFormDto toDto(MedicalFormView view);
}
