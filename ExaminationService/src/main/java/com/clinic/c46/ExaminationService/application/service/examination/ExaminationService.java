package com.clinic.c46.ExaminationService.application.service.examination;


import com.clinic.c46.ExaminationService.application.service.examination.dto.ExamResultDto;

import java.util.concurrent.CompletableFuture;

public interface ExaminationService {
    
    CompletableFuture<Void> createResult(String staffId, ExamResultDto examResultDto);
}
