package com.clinic.c46.NotificationService.application.dto;

import com.clinic.c46.NotificationService.application.service.EmailTemplateVariables;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public record ExamResultTemplateVariables(
        String patientName,
        String examinationId,
        String completionDate,
        String resultHtmlContent) implements EmailTemplateVariables {
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientName", patientName);
        map.put("examinationId", examinationId);
        map.put("completionDate", completionDate);
        map.put("resultHtmlContent", resultHtmlContent);
        return map;
    }
}
