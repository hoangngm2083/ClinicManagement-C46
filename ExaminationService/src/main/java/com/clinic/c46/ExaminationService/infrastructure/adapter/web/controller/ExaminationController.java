package com.clinic.c46.ExaminationService.infrastructure.adapter.web.controller;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.ExaminationService.application.dto.ExamsPagedDto;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.ExaminationService.domain.query.SearchExamsQuery;
import lombok.RequiredArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/examination")
@RequiredArgsConstructor
@Validated
public class ExaminationController {

    private final QueryGateway queryGateway;


    @GetMapping
    public CompletableFuture<ResponseEntity<ExamsPagedDto>> searchExaminations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page) {

        SearchExamsQuery query = SearchExamsQuery.builder()
                .keyword(keyword)
                .page(page)
                .build();
        return queryGateway.query(query, ResponseTypes.instanceOf(ExamsPagedDto.class))
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{examId}")
    public CompletableFuture<ResponseEntity<ExamDetailsDto>> getExaminationById(
            @PathVariable String examId) {

        GetExaminationByIdQuery query = GetExaminationByIdQuery.builder()
                .examinationId(examId)
                .build();
        return queryGateway.query(query, ResponseTypes.instanceOf(ExamDetailsDto.class))
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/medical-result")
    public CompletableFuture<ResponseEntity<ExamsPagedDto>> createMedicalResult() {
        return null;
    }

}