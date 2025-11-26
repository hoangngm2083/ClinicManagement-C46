package com.clinic.c46.ExaminationService.infrastructure.adapter.rest.controller;

import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.query.examination.GetExaminationByIdQuery;
import com.clinic.c46.ExaminationService.application.dto.ExamsPagedDto;
import com.clinic.c46.ExaminationService.application.service.examination.ExaminationService;
import com.clinic.c46.ExaminationService.application.service.examination.dto.ExamResultDto;
import com.clinic.c46.ExaminationService.domain.query.SearchExamsQuery;
import com.clinic.c46.ExaminationService.infrastructure.adapter.rest.dto.CreateResultRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/examination")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ExaminationController {

    private final QueryGateway queryGateway;
    private final ExaminationService examinationService;

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
    public CompletableFuture<ResponseEntity<ExamDetailsDto>> getExaminationById(@PathVariable String examId) {

        GetExaminationByIdQuery query = GetExaminationByIdQuery.builder()
                .examinationId(examId)
                .build();
        return queryGateway.query(query, ResponseTypes.instanceOf(ExamDetailsDto.class))
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{examId}/result")
    public CompletableFuture<ResponseEntity<Void>> createResult(
            @PathVariable @NotBlank(message = "Mã hồ sơ không được trống") String examId,
            @Valid @RequestBody CreateResultRequest request,
            @RequestHeader("Staff-Id") @NotBlank(message = "Mã nhân viên không được trống") String staffId) {

        log.info("[ExaminationController] Received create result request from staffId: {}, examId: {}", staffId,
                examId);

        ExamResultDto examResultDto = ExamResultDto.builder()
                .examId(examId)
                .doctorId(staffId)
                .serviceId(request.serviceId())
                .data(request.data())
                .build();

        return examinationService.createResult(staffId, examResultDto)
                .thenApply(result -> ResponseEntity
                        .created(URI.create("/api/examination/" + examId + "/result"))

                        .build());
    }

}