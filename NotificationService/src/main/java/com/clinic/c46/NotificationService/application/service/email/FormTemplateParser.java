package com.clinic.c46.NotificationService.application.service.email;

import com.fasterxml.jackson.databind.JsonNode;

public interface FormTemplateParser {
    String parse(JsonNode formTemplate, JsonNode resultData);
}
