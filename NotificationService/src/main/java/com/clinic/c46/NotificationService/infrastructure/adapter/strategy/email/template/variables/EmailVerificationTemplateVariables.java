package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;


@Builder
public record EmailVerificationTemplateVariables(String callbackUrl) implements EmailTemplateVariables {

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("callbackUrl", callbackUrl);
        return map;
    }
}
