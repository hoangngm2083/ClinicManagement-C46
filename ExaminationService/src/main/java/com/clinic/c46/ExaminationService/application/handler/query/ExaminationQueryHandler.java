package com.clinic.c46.ExaminationService.application.handler.query;


import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.ExaminationService.application.dto.ExamViewDto;
import com.clinic.c46.ExaminationService.application.dto.ExamsPagedDto;
import com.clinic.c46.ExaminationService.domain.query.SearchExamsQuery;
import com.clinic.c46.ExaminationService.infrastructure.adapter.helper.ExamMapper;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository.ExamViewRepository;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.view.ExamView;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExaminationQueryHandler {
    private final ExamViewRepository examViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;
    private final ExamMapper examMapper;


    @QueryHandler
    public ExamsPagedDto handle(SearchExamsQuery q) {
        Pageable pageable = pageAndSortHelper.buildPageable(q.page(), "", SortDirection.ASC);

        Specification<ExamView> spec = specificationBuilder.keyword(q.keyword(),
                List.of("patientName", "patientEmail"));
        Page<ExamView> pageResult = examViewRepository.findAll(spec, pageable);


        return pageAndSortHelper.toPaged(pageResult, view -> ExamViewDto.builder()
                .id(view.getId())
                .patientId(view.getPatientId())
                .patientName(view.getPatientName())
                .patientEmail(view.getPatientEmail())
                .medicalFormId(view.getMedicalFormId())
                .build(), ExamsPagedDto::new);
    }

    @QueryHandler
    public Optional<ExamDetailsDto> handle(GetExaminationByIdQuery query) {
        return examViewRepository.findByIdWithResults(query.examinationId())
                .map(examMapper::toExamDetailsDto);
    }

}
