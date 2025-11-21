package com.clinic.c46.ExaminationService.application.handler.query;


import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.helper.SpecificationBuilder;
import com.clinic.c46.ExaminationService.application.dto.ExamViewDto;
import com.clinic.c46.ExaminationService.application.dto.ExamsPagedDto;
import com.clinic.c46.ExaminationService.application.dto.MedicalResultViewDto;
import com.clinic.c46.ExaminationService.application.repository.ExamViewRepository;
import com.clinic.c46.ExaminationService.domain.query.SearchExamsQuery;
import com.clinic.c46.ExaminationService.domain.view.ExamView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExaminationQueryHandler {
    private final ExamViewRepository examViewRepository;
    private final PageAndSortHelper pageAndSortHelper;
    private final SpecificationBuilder specificationBuilder;


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
                .results(view.getResults()
                        .stream()
                        .map(resultView -> MedicalResultViewDto.builder()
                                .doctorId(resultView.getDoctorId())
                                .doctorName(resultView.getDoctorName())
                                .status(resultView.getStatus())
                                .data(resultView.getData())
                                .pdfUrl(resultView.getPdfUrl())
                                .build())
                        .collect(Collectors.toSet()))
                .build(), ExamsPagedDto::new);
    }

}
