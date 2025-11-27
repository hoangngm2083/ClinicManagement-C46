package com.clinic.c46.NotificationService.application.service.email.variables;

import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
public record ExamResultTemplateVariables(String patientName, String examinationId, String completionDate,
                                          List<ResultItem> resultItems) implements EmailTemplateVariables {

    @Builder
    public record ResultItem(String serviceName, String doctorName, String resultHtmlContent) {
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientName", patientName);
        map.put("examinationId", examinationId);
        map.put("completionDate", completionDate);
        map.put("resultItems", resultItems);
        return map;
    }

}
