package com.clinic.c46.NotificationService.domain.type;

import com.clinic.c46.NotificationService.application.service.EmailTemplateVariables;
import com.clinic.c46.NotificationService.application.service.EmailVerificationTemplateVariables;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailTemplate {
    VERIFY_EMAIL("email-verification", "Xác thực địa chỉ Email.", EmailVerificationTemplateVariables.class);

    private final String fileName;
    private final String subject;
    private final Class<? extends EmailTemplateVariables> variableClass;
}

