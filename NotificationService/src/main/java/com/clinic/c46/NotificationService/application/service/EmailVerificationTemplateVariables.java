package com.clinic.c46.NotificationService.application.service;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class EmailVerificationTemplateVariables implements EmailTemplateVariables {
    private String callbackUrl;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("callbackUrl", callbackUrl);
        return map;
    }
}
